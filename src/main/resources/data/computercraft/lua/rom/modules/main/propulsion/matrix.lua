--[[
Parts of this file (inverse, determinant, orthonormalize, diagonalize) are ported from Godot Engine's
core/math/basis.cpp.

Copyright (c) 2014-present Godot Engine contributors (see https://godotengine.org/license).
Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Matrices are tables of rows (m[row][col]) acting on column vectors: m * v.
Tables returned by ship.getInertiaTensor() can be passed to matrix.new directly.
]]

local expect = require("cc.expect").expect

local sqrt, abs, atan, sin, cos = math.sqrt, math.abs, math.atan, math.sin, math.cos
local CMP_EPSILON = 0.00001

local matrix = {}
local Matrix = {}
local metatable = { __index = Matrix, __name = "matrix" }

local function make(rows) return setmetatable(rows, metatable) end

local function rows9(a, b, c, d, e, f, g, h, i)
    return make({ { a, b, c }, { d, e, f }, { g, h, i } })
end

local function vec(x, y, z) return vector.new(x, y, z) end

local function isVector(v) return type(v) == "table" and type(v.x) == "number" and type(v.y) == "number" and type(v.z) == "number" end

function matrix.new(a, ...)
    if a == nil then return matrix.identity() end
    if type(a) == "table" then
        local r = {}
        for i = 1, 3 do
            expect(1, a[i], "table")
            r[i] = { a[i][1], a[i][2], a[i][3] }
        end
        return make(r)
    end
    return rows9(a, ...)
end

function matrix.identity() return rows9(1, 0, 0, 0, 1, 0, 0, 0, 1) end

function matrix.zero() return rows9(0, 0, 0, 0, 0, 0, 0, 0, 0) end

function matrix.fromScale(s)
    if not isVector(s) then error("bad argument #1 (vector expected)", 2) end
    return rows9(s.x, 0, 0, 0, s.y, 0, 0, 0, s.z)
end

function matrix.fromColumns(x, y, z)
    for i, v in ipairs({ x, y, z }) do
        if not isVector(v) then error(("bad argument #%d (vector expected)"):format(i), 2) end
    end
    return rows9(x.x, y.x, z.x, x.y, y.y, z.y, x.z, y.z, z.z)
end

function matrix.fromQuaternion(q)
    expect(1, q, "table")
    return make(require("propulsion.quaternion")._toRows(q))
end

function Matrix:copy() return matrix.new(self) end

function Matrix:row(i) return vec(self[i][1], self[i][2], self[i][3]) end

function Matrix:column(i) return vec(self[1][i], self[2][i], self[3][i]) end

function Matrix:transpose()
    return rows9(self[1][1], self[2][1], self[3][1],
                 self[1][2], self[2][2], self[3][2],
                 self[1][3], self[2][3], self[3][3])
end

function Matrix:determinant()
    return self[1][1] * (self[2][2] * self[3][3] - self[3][2] * self[2][3])
         - self[2][1] * (self[1][2] * self[3][3] - self[3][2] * self[1][3])
         + self[3][1] * (self[1][2] * self[2][3] - self[2][2] * self[1][3])
end

function Matrix:inverse()
    local r = self
    local function cofac(r1, c1, r2, c2) return r[r1][c1] * r[r2][c2] - r[r1][c2] * r[r2][c1] end
    local co1, co2, co3 = cofac(2, 2, 3, 3), cofac(2, 3, 3, 1), cofac(2, 1, 3, 2)
    local det = r[1][1] * co1 + r[1][2] * co2 + r[1][3] * co3
    if det == 0 then error("matrix is singular", 2) end
    local s = 1 / det
    return rows9(co1 * s, cofac(1, 3, 3, 2) * s, cofac(1, 2, 2, 3) * s,
                 co2 * s, cofac(1, 1, 3, 3) * s, cofac(1, 3, 2, 1) * s,
                 co3 * s, cofac(1, 2, 3, 1) * s, cofac(1, 1, 2, 2) * s)
end

function Matrix:mul(o)
    if type(o) == "number" then
        local r = {}
        for i = 1, 3 do r[i] = { self[i][1] * o, self[i][2] * o, self[i][3] * o } end
        return make(r)
    elseif isVector(o) then
        return self:transform(o)
    elseif type(o) == "table" then
        local r = {}
        for i = 1, 3 do
            r[i] = {}
            for j = 1, 3 do
                r[i][j] = self[i][1] * o[1][j] + self[i][2] * o[2][j] + self[i][3] * o[3][j]
            end
        end
        return make(r)
    end
    error("bad argument #1 (number, vector or matrix expected)", 2)
end

function Matrix:add(o)
    local r = {}
    for i = 1, 3 do r[i] = { self[i][1] + o[i][1], self[i][2] + o[i][2], self[i][3] + o[i][3] } end
    return make(r)
end

function Matrix:sub(o) return self:add(o:mul(-1)) end

function Matrix:transform(v)
    if not isVector(v) then error("bad argument #1 (vector expected)", 2) end
    return vec(self[1][1] * v.x + self[1][2] * v.y + self[1][3] * v.z,
               self[2][1] * v.x + self[2][2] * v.y + self[2][3] * v.z,
               self[3][1] * v.x + self[3][2] * v.y + self[3][3] * v.z)
end

function Matrix:transformInverse(v)
    if not isVector(v) then error("bad argument #1 (vector expected)", 2) end
    return vec(self[1][1] * v.x + self[2][1] * v.y + self[3][1] * v.z,
               self[1][2] * v.x + self[2][2] * v.y + self[3][2] * v.z,
               self[1][3] * v.x + self[2][3] * v.y + self[3][3] * v.z)
end

function Matrix:orthonormalize()
    local x, y, z = self:column(1), self:column(2), self:column(3)
    x = x:normalize()
    y = (y - x * x:dot(y)):normalize()
    z = (z - x * x:dot(z) - y * y:dot(z)):normalize()
    return matrix.fromColumns(x, y, z)
end

function Matrix:diagonalize()
    local m = self:copy()
    local accRot = matrix.identity()
    local offNorm2 = m[1][2] ^ 2 + m[1][3] ^ 2 + m[2][3] ^ 2
    local ite = 0
    while offNorm2 > CMP_EPSILON * CMP_EPSILON and ite < 1024 do
        ite = ite + 1
        local el01, el02, el12 = m[1][2] ^ 2, m[1][3] ^ 2, m[2][3] ^ 2
        local i, j
        if el01 > el02 then
            if el12 > el01 then i, j = 2, 3 else i, j = 1, 2 end
        else
            if el12 > el02 then i, j = 2, 3 else i, j = 1, 3 end
        end

        local angle
        local a, b = m[j][j], m[i][i]
        local tol = math.max(CMP_EPSILON * abs(a), CMP_EPSILON)
        if a == b or abs(a - b) < tol then
            angle = math.pi / 4
        else
            angle = 0.5 * atan(2 * m[i][j] / (m[j][j] - m[i][i]))
        end

        local rot = matrix.identity()
        rot[i][i], rot[j][j] = cos(angle), cos(angle)
        rot[j][i] = sin(angle)
        rot[i][j] = -rot[j][i]

        offNorm2 = offNorm2 - m[i][j] ^ 2

        m = rot:mul(m):mul(rot:transpose())
        accRot = rot:mul(accRot)
    end
    return { m[1][1], m[2][2], m[3][3] }, { accRot:row(1), accRot:row(2), accRot:row(3) }
end

function Matrix:toQuaternion()
    return require("propulsion.quaternion")._fromRows(self)
end

function Matrix:toEuler()
    return self:toQuaternion():toEuler()
end

function Matrix:tostring()
    return ("matrix([%g, %g, %g], [%g, %g, %g], [%g, %g, %g])"):format(
        self[1][1], self[1][2], self[1][3], self[2][1], self[2][2], self[2][3], self[3][1], self[3][2], self[3][3])
end

metatable.__mul = function(a, b)
    if type(a) == "number" then return b:mul(a) end
    return a:mul(b)
end
metatable.__add = function(a, b) return a:add(b) end
metatable.__sub = function(a, b) return a:sub(b) end
metatable.__unm = function(a) return a:mul(-1) end
metatable.__tostring = Matrix.tostring

return matrix
