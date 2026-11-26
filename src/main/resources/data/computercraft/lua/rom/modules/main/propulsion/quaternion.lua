--[[
Parts of this file (fromEuler, toEuler, lookAt) are ported from Godot Engine's core/math/basis.cpp.

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

Conventions (same as the `ship` API): Y up, right-handed, -Z is forward. Angles are degrees.
heading = compass (0 = -Z/north, 90 = +X/east), pitch = nose up, roll = right side down.
Vectors are returned as CC `vector` objects; any table with x, y, z fields is accepted.
]]

local expect = require("cc.expect").expect

local sqrt, sin, cos, atan2, asin, acos, abs, rad, deg =
    math.sqrt, math.sin, math.cos, math.atan2 or math.atan, math.asin, math.acos, math.abs, math.rad, math.deg

local quaternion = {}
local Quaternion = {}
local metatable = { __index = Quaternion, __name = "quaternion" }

local function make(x, y, z, w)
    return setmetatable({ x = x, y = y, z = z, w = w }, metatable)
end

local function vec(x, y, z) return vector.new(x, y, z) end

local function checkVector(value, index)
    expect(index, value, "table")
    if type(value.x) ~= "number" or type(value.y) ~= "number" or type(value.z) ~= "number" then
        error(("bad argument #%d (vector expected)"):format(index), 3)
    end
    return value
end

function quaternion.new(x, y, z, w)
    if type(x) == "table" then
        return make(x.x or 0, x.y or 0, x.z or 0, x.w or 1)
    end
    return make(x or 0, y or 0, z or 0, w or 1)
end

function quaternion.identity() return make(0, 0, 0, 1) end

function quaternion.fromAxisAngle(axis, angle)
    checkVector(axis, 1)
    expect(2, angle, "number")
    local len = sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z)
    if len < 1e-12 then return quaternion.identity() end
    local half = rad(angle) / 2
    local s = sin(half) / len
    return make(axis.x * s, axis.y * s, axis.z * s, cos(half))
end

function quaternion.fromRotationVector(v)
    checkVector(v, 1)
    local angle = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
    if angle < 1e-12 then return quaternion.identity() end
    return quaternion.fromAxisAngle(v, angle)
end

local function toRows(q)
    local d = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w
    local s = 2 / d
    local xs, ys, zs = q.x * s, q.y * s, q.z * s
    local wx, wy, wz = q.w * xs, q.w * ys, q.w * zs
    local xx, xy, xz = q.x * xs, q.x * ys, q.x * zs
    local yy, yz, zz = q.y * ys, q.y * zs, q.z * zs
    return {
        { 1 - (yy + zz), xy - wz, xz + wy },
        { xy + wz, 1 - (xx + zz), yz - wx },
        { xz - wy, yz + wx, 1 - (xx + yy) },
    }
end

local function fromRows(m)
    local trace = m[1][1] + m[2][2] + m[3][3]
    local t = {}
    if trace > 0 then
        local s = sqrt(trace + 1)
        t[4] = s * 0.5
        s = 0.5 / s
        t[1] = (m[3][2] - m[2][3]) * s
        t[2] = (m[1][3] - m[3][1]) * s
        t[3] = (m[2][1] - m[1][2]) * s
    else
        local i
        if m[1][1] < m[2][2] then
            i = m[2][2] < m[3][3] and 3 or 2
        else
            i = m[1][1] < m[3][3] and 3 or 1
        end
        local j = i % 3 + 1
        local k = j % 3 + 1
        local s = sqrt(m[i][i] - m[j][j] - m[k][k] + 1)
        t[i] = s * 0.5
        s = 0.5 / s
        t[4] = (m[k][j] - m[j][k]) * s
        t[j] = (m[j][i] + m[i][j]) * s
        t[k] = (m[k][i] + m[i][k]) * s
    end
    return make(t[1], t[2], t[3], t[4]):normalize()
end

quaternion._toRows, quaternion._fromRows = toRows, fromRows

function quaternion.fromMatrix(m)
    expect(1, m, "table")
    return fromRows(m)
end

local function mul3(a, b)
    local r = {}
    for i = 1, 3 do
        r[i] = {}
        for j = 1, 3 do
            r[i][j] = a[i][1] * b[1][j] + a[i][2] * b[2][j] + a[i][3] * b[3][j]
        end
    end
    return r
end

function quaternion.fromEuler(heading, pitch, roll)
    expect(1, heading, "number")
    expect(2, pitch, "number", "nil")
    expect(3, roll, "number", "nil")
    local x, y, z = rad(pitch or 0), -rad(heading), -rad(roll or 0)
    local c, s = cos(x), sin(x)
    local xmat = { { 1, 0, 0 }, { 0, c, -s }, { 0, s, c } }
    c, s = cos(y), sin(y)
    local ymat = { { c, 0, s }, { 0, 1, 0 }, { -s, 0, c } }
    c, s = cos(z), sin(z)
    local zmat = { { c, -s, 0 }, { s, c, 0 }, { 0, 0, 1 } }
    return fromRows(mul3(ymat, mul3(xmat, zmat)))
end

function quaternion.lookAt(forward, up)
    checkVector(forward, 1)
    up = up or vec(0, 1, 0)
    checkVector(up, 2)
    local fl = sqrt(forward.x ^ 2 + forward.y ^ 2 + forward.z ^ 2)
    if fl < 1e-12 then error("bad argument #1 (forward can't be zero)", 2) end
    local zx, zy, zz = -forward.x / fl, -forward.y / fl, -forward.z / fl
    local xx, xy, xz = up.y * zz - up.z * zy, up.z * zx - up.x * zz, up.x * zy - up.y * zx
    local xl = sqrt(xx * xx + xy * xy + xz * xz)
    if xl < 1e-9 then
        if abs(zx) < 0.9 then xx, xy, xz = 0, -zz, zy else xx, xy, xz = zz, 0, -zx end
        xl = sqrt(xx * xx + xy * xy + xz * xz)
    end
    xx, xy, xz = xx / xl, xy / xl, xz / xl
    local yx, yy, yz = zy * xz - zz * xy, zz * xx - zx * xz, zx * xy - zy * xx
    return fromRows({ { xx, yx, zx }, { xy, yy, zy }, { xz, yz, zz } })
end

function quaternion.rotationTo(from, to)
    checkVector(from, 1)
    checkVector(to, 2)
    local fl = sqrt(from.x ^ 2 + from.y ^ 2 + from.z ^ 2)
    local tl = sqrt(to.x ^ 2 + to.y ^ 2 + to.z ^ 2)
    if fl < 1e-12 or tl < 1e-12 then return quaternion.identity() end
    local ax, ay, az = from.x / fl, from.y / fl, from.z / fl
    local bx, by, bz = to.x / tl, to.y / tl, to.z / tl
    local d = ax * bx + ay * by + az * bz
    if d < -0.999999 then
        local px, py, pz
        if abs(ax) < 0.9 then px, py, pz = 0, -az, ay else px, py, pz = az, 0, -ax end
        local pl = sqrt(px * px + py * py + pz * pz)
        return make(px / pl, py / pl, pz / pl, 0)
    end
    local cx, cy, cz = ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx
    return make(cx, cy, cz, 1 + d):normalize()
end

function Quaternion:length() return sqrt(self.x ^ 2 + self.y ^ 2 + self.z ^ 2 + self.w ^ 2) end

function Quaternion:normalize()
    local l = self:length()
    if l < 1e-12 then return quaternion.identity() end
    return make(self.x / l, self.y / l, self.z / l, self.w / l)
end

function Quaternion:conjugate() return make(-self.x, -self.y, -self.z, self.w) end

function Quaternion:inverse()
    local d = self.x ^ 2 + self.y ^ 2 + self.z ^ 2 + self.w ^ 2
    return make(-self.x / d, -self.y / d, -self.z / d, self.w / d)
end

function Quaternion:dot(o) return self.x * o.x + self.y * o.y + self.z * o.z + self.w * o.w end

function Quaternion:mul(o)
    return make(
        self.w * o.x + self.x * o.w + self.y * o.z - self.z * o.y,
        self.w * o.y - self.x * o.z + self.y * o.w + self.z * o.x,
        self.w * o.z + self.x * o.y - self.y * o.x + self.z * o.w,
        self.w * o.w - self.x * o.x - self.y * o.y - self.z * o.z)
end

function Quaternion:rotate(v)
    checkVector(v, 1)
    local ux, uy, uz, w = self.x, self.y, self.z, self.w
    local tx = 2 * (uy * v.z - uz * v.y)
    local ty = 2 * (uz * v.x - ux * v.z)
    local tz = 2 * (ux * v.y - uy * v.x)
    return vec(
        v.x + w * tx + (uy * tz - uz * ty),
        v.y + w * ty + (uz * tx - ux * tz),
        v.z + w * tz + (ux * ty - uy * tx))
end

function Quaternion:rotateInverse(v) return self:conjugate():rotate(v) end

function Quaternion:slerp(o, t)
    expect(2, t, "number")
    local d = self:dot(o)
    local ox, oy, oz, ow = o.x, o.y, o.z, o.w
    if d < 0 then d, ox, oy, oz, ow = -d, -ox, -oy, -oz, -ow end
    local a, b
    if d > 0.9995 then
        a, b = 1 - t, t
    else
        local theta = acos(d)
        local s = sin(theta)
        a, b = sin((1 - t) * theta) / s, sin(t * theta) / s
    end
    return make(a * self.x + b * ox, a * self.y + b * oy, a * self.z + b * oz, a * self.w + b * ow):normalize()
end

function Quaternion:toAxisAngle()
    local q = self:normalize()
    if q.w < 0 then q = make(-q.x, -q.y, -q.z, -q.w) end
    local s = sqrt(q.x * q.x + q.y * q.y + q.z * q.z)
    if s < 1e-12 then return vec(0, 1, 0), 0 end
    return vec(q.x / s, q.y / s, q.z / s), deg(2 * atan2(s, q.w))
end

function Quaternion:toRotationVector()
    local axis, angle = self:toAxisAngle()
    return axis * angle
end

function Quaternion:toEuler()
    local m = toRows(self)
    local eps = 0.00000025
    local x, y, z
    local m12 = m[2][3]
    if m12 < 1 - eps then
        if m12 > -(1 - eps) then
            if m[2][1] == 0 and m[1][2] == 0 and m[1][3] == 0 and m[3][1] == 0 and m[1][1] == 1 then
                x, y, z = atan2(-m12, m[2][2]), 0, 0
            else
                x, y, z = asin(-m12), atan2(m[1][3], m[3][3]), atan2(m[2][1], m[2][2])
            end
        else
            x, y, z = math.pi * 0.5, atan2(m[1][2], m[1][1]), 0
        end
    else
        x, y, z = -math.pi * 0.5, -atan2(m[1][2], m[1][1]), 0
    end
    local heading = (-deg(y)) % 360
    local roll = ((-deg(z)) + 180) % 360 - 180
    return heading + 0.0, deg(x) + 0.0, roll + 0.0
end

function Quaternion:axes()
    return self:rotate(vec(1, 0, 0)), self:rotate(vec(0, 1, 0)), self:rotate(vec(0, 0, -1))
end

function Quaternion:toMatrix()
    return require("propulsion.matrix").fromQuaternion(self)
end

function Quaternion:tostring()
    return ("quaternion(%g, %g, %g, %g)"):format(self.x, self.y, self.z, self.w)
end

metatable.__mul = function(a, b)
    if getmetatable(b) == metatable then return a:mul(b) end
    return a:rotate(b)
end
metatable.__unm = function(a) return make(-a.x, -a.y, -a.z, -a.w) end
metatable.__eq = function(a, b) return a.x == b.x and a.y == b.y and a.z == b.z and a.w == b.w end
metatable.__tostring = Quaternion.tostring

return quaternion
