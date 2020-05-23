package info.laht.acco.math

import kotlin.math.*

data class Quaternion(
    private var _x: Double = 0.0,
    private var _y: Double = 0.0,
    private var _z: Double = 0.0,
    private var _w: Double = 1.0
) : Cloneable {

    var x: Double
        get() = _x
        set(value) {
            _x = value
            onChangeCallback?.invoke()
        }

    var y: Double
        get() = _y
        set(value) {
            _y = value
            onChangeCallback?.invoke()
        }

    var z: Double
        get() = _z
        set(value) {
            _z = value
            onChangeCallback?.invoke()
        }

    var w: Double
        get() = _w
        set(value) {
            _w = value
            onChangeCallback?.invoke()
        }

    internal var onChangeCallback: (() -> Unit)? = null

    /**
     * Sets values of this quaternion.
     */
    fun set(x: Double, y: Double, z: Double, w: Double): Quaternion {
        this._x = x
        this._y = y
        this._z = z
        this._w = w
        this.onChangeCallback?.invoke()
        return this
    }

    public override fun clone() = copy()

    /**
     * Copies values of q to this quaternion.
     */
    fun copy(q: Quaternion): Quaternion {
        return set(q._x, q._y, q._z, q._w)
    }

    /**
     * Sets this quaternion from rotation specified by Euler angles.
     */
    @JvmOverloads
    fun setFromEuler(euler: Euler, update: Boolean = true): Quaternion {
        val x = euler.x
        val y = euler.y
        val z = euler.z

        // http://www.mathworks.com/matlabcentral/fileexchange/
        // 	20696-function-to-convert-between-dcm-euler-angles-quaternions-and-euler-vectors/
        //	content/SpinCalc.m

        val c1 = cos(x / 2)
        val c2 = cos(y / 2)
        val c3 = cos(z / 2)

        val s1 = sin(x / 2)
        val s2 = sin(y / 2)
        val s3 = sin(z / 2)


        when (euler.order) {
            EulerOrder.XYZ -> {

                this._x = s1 * c2 * c3 + c1 * s2 * s3
                this._y = c1 * s2 * c3 - s1 * c2 * s3
                this._z = c1 * c2 * s3 + s1 * s2 * c3
                this._w = c1 * c2 * c3 - s1 * s2 * s3

            }
            EulerOrder.YXZ -> {

                this._x = s1 * c2 * c3 + c1 * s2 * s3
                this._y = c1 * s2 * c3 - s1 * c2 * s3
                this._z = c1 * c2 * s3 - s1 * s2 * c3
                this._w = c1 * c2 * c3 + s1 * s2 * s3

            }
            EulerOrder.ZXY -> {

                this._x = s1 * c2 * c3 - c1 * s2 * s3
                this._y = c1 * s2 * c3 + s1 * c2 * s3
                this._z = c1 * c2 * s3 + s1 * s2 * c3
                this._w = c1 * c2 * c3 - s1 * s2 * s3

            }
            EulerOrder.ZYX -> {

                this._x = s1 * c2 * c3 - c1 * s2 * s3
                this._y = c1 * s2 * c3 + s1 * c2 * s3
                this._z = c1 * c2 * s3 - s1 * s2 * c3
                this._w = c1 * c2 * c3 + s1 * s2 * s3

            }
            EulerOrder.YZX -> {

                this._x = s1 * c2 * c3 + c1 * s2 * s3
                this._y = c1 * s2 * c3 + s1 * c2 * s3
                this._z = c1 * c2 * s3 - s1 * s2 * c3
                this._w = c1 * c2 * c3 - s1 * s2 * s3

            }
            EulerOrder.XZY -> {

                this._x = s1 * c2 * c3 - c1 * s2 * s3
                this._y = c1 * s2 * c3 - s1 * c2 * s3
                this._z = c1 * c2 * s3 + s1 * s2 * c3
                this._w = c1 * c2 * c3 + s1 * s2 * s3

            }
        }

        if (update) {
            this.onChangeCallback?.invoke()
        }

        return this
    }

    fun setFromAxisAngle(axisX: Double, axisY: Double, axisZ: Double, angle: Double): Quaternion {
        val halfAngle = angle / 2
        val s = sin(halfAngle)

        this._x = axisX * s
        this._y = axisY * s
        this._z = axisZ * s
        this._w = cos(halfAngle)

        this.onChangeCallback?.invoke()

        return this
    }

    /**
     * Sets this quaternion from rotation specified by axis and angle.
     * Adapted from http://www.euclideanspace.com/maths/geometry/rotations/conversions/angleToQuaternion/index.htm.
     * Axis have to be normalized, angle is in radians.
     */
    fun setFromAxisAngle(axis: Vector3, angle: Double): Quaternion {
        return this.setFromAxisAngle(axis.x, axis.y, axis.z, angle)
    }

    /**
     * Sets this quaternion from rotation component of m. Adapted from http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm.
     */
    fun setFromRotationMatrix(m: Matrix4): Quaternion {
        // http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm

        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)

        val te = m.elements

        val m11 = te[0]
        val m12 = te[4]
        val m13 = te[8]
        val m21 = te[1]
        val m22 = te[5]
        val m23 = te[9]
        val m31 = te[2]
        val m32 = te[6]
        val m33 = te[10]

        val trace = m11 + m22 + m33
        val s: Double

        if (trace > 0) {

            s = 0.5 / sqrt(trace + 1)

            this._w = 0.25 / s
            this._x = (m32 - m23) * s
            this._y = (m13 - m31) * s
            this._z = (m21 - m12) * s

        } else if (m11 > m22 && m11 > m33) {

            s = 2 * sqrt(1 + m11 - m22 - m33)

            this._w = (m32 - m23) / s
            this._x = 0.25 * s
            this._y = (m12 + m21) / s
            this._z = (m13 + m31) / s

        } else if (m22 > m33) {

            s = 2 * sqrt(1 + m22 - m11 - m33)

            this._w = (m13 - m31) / s
            this._x = (m12 + m21) / s
            this._y = 0.25 * s
            this._z = (m23 + m32) / s

        } else {

            s = 2 * sqrt(1 + m33 - m11 - m22)

            this._w = (m21 - m12) / s
            this._x = (m13 + m31) / s
            this._y = (m23 + m32) / s
            this._z = 0.25 * s

        }

        this.onChangeCallback?.invoke()

        return this
    }

    fun setFromUnitVectors(vFrom: Vector3, vTo: Vector3): Quaternion {
        // assumes direction vectors vFrom and vTo are normalized

        val EPS = 0.000001

        var r = vFrom.dot(vTo) + 1

        if (r < EPS) {

            r = 0.0

            if (abs(vFrom.x) > abs(vFrom.z)) {

                this._x = -vFrom.y
                this._y = vFrom.x
                this._z = 0.0
                this._w = r

            } else {

                this._x = 0.0
                this._y = -vFrom.z
                this._z = vFrom.y
                this._w = r

            }

        } else {

            // crossVectors( vFrom, vTo ); // inlined to avoid cyclic dependency on Vector3

            this._x = vFrom.y * vTo.z - vFrom.z * vTo.y
            this._y = vFrom.z * vTo.x - vFrom.x * vTo.z
            this._z = vFrom.x * vTo.y - vFrom.y * vTo.x
            this._w = r

        }

        return this.normalize()
    }

    fun angleTo(q: Quaternion): Double {
        return 2 * acos(abs(clamp(this.dot(q), -1.0, 1.0)))
    }

    fun rotateTowards(q: Quaternion, step: Double): Quaternion {
        val angle = this.angleTo(q)

        if (angle == 0.0) return this

        val t = min(1.0, step / angle)

        this.slerp(q, t)

        return this
    }

    /**
     * Inverts this quaternion.
     */
    fun inverse(): Quaternion {
        return this.conjugate()
    }

    fun conjugate(): Quaternion {
        this._x *= -1
        this._y *= -1
        this._z *= -1

        this.onChangeCallback?.invoke()

        return this
    }

    fun dot(v: Quaternion): Double {
        return this._x * v._x + this._y * v._y + this._z * v._z + this._w * v._w
    }

    fun lengthSq(): Double {
        return this._x * this._x + this._y * this._y + this._z * this._z + this._w * this._w
    }

    /**
     * Computes length of this quaternion.
     */
    fun length(): Double {
        return sqrt(this._x * this._x + this._y * this._y + this._z * this._z + this._w * this._w)

    }

    /**
     * Normalizes this quaternion.
     */
    fun normalize(): Quaternion {
        var l = this.length()

        if (l == 0.0) {

            this._x = 0.0
            this._y = 0.0
            this._z = 0.0
            this._w = 1.0

        } else {

            l = 1.0 / l

            this._x = this._x * l
            this._y = this._y * l
            this._z = this._z * l
            this._w = this._w * l

        }

        this.onChangeCallback?.invoke()

        return this
    }

    /**
     * Multiplies this quaternion by b.
     */
    fun multiply(q: Quaternion): Quaternion {
        return this.multiplyQuaternions(this, q)
    }

    fun premultiply(q: Quaternion): Quaternion {
        return this.multiplyQuaternions(q, this)
    }

    /**
     * Sets this quaternion to a x b
     * Adapted from http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm.
     */
    fun multiplyQuaternions(a: Quaternion, b: Quaternion): Quaternion {
        val qax = a._x
        val qay = a._y
        val qaz = a._z
        val qaw = a._w
        val qbx = b._x
        val qby = b._y
        val qbz = b._z
        val qbw = b._w

        this._x = qax * qbw + qaw * qbx + qay * qbz - qaz * qby
        this._y = qay * qbw + qaw * qby + qaz * qbx - qax * qbz
        this._z = qaz * qbw + qaw * qbz + qax * qby - qay * qbx
        this._w = qaw * qbw - qax * qbx - qay * qby - qaz * qbz

        this.onChangeCallback?.invoke()

        return this
    }

    fun slerp(qb: Quaternion, t: Double): Quaternion {
        if (t == 0.0) {
            return this
        }
        if (t == 1.0) {
            return this.copy(qb)
        }

        val x = this._x
        val y = this._y
        val z = this._z
        val w = this._w

        // http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/slerp/

        var cosHalfTheta = w * qb._w + x * qb._x + y * qb._y + z * qb._z

        if (cosHalfTheta < 0) {

            this._w = -qb._w
            this._x = -qb._x
            this._y = -qb._y
            this._z = -qb._z

            cosHalfTheta = -cosHalfTheta

        } else {

            this.copy(qb)

        }

        if (cosHalfTheta >= 1.0) {

            this._w = w
            this._x = x
            this._y = y
            this._z = z

            return this

        }

        val sqrSinHalfTheta = 1.0 - cosHalfTheta * cosHalfTheta

        if (sqrSinHalfTheta <= 1e-6) {

            val s = 1 - t
            this._w = s * w + t * this._w
            this._x = s * x + t * this._x
            this._y = s * y + t * this._y
            this._z = s * z + t * this._z

            this.normalize()
            this.onChangeCallback?.invoke()

            return this

        }

        val sinHalfTheta = sqrt(sqrSinHalfTheta)
        val halfTheta = atan2(sinHalfTheta, cosHalfTheta)
        val ratioA = sin((1 - t) * halfTheta) / sinHalfTheta
        val ratioB = sin(t * halfTheta) / sinHalfTheta

        this._w = (w * ratioA + this._w * ratioB)
        this._x = (x * ratioA + this._x * ratioB)
        this._y = (y * ratioA + this._y * ratioB)
        this._z = (z * ratioA + this._z * ratioB)

        this.onChangeCallback?.invoke()

        return this
    }


    @JvmOverloads
    fun fromArray(array: DoubleArray, offset: Int = 0): Quaternion {
        _x = array[offset + 0]
        _y = array[offset + 1]
        _z = array[offset + 2]
        _w = array[offset + 3]
        return this
    }

    @JvmOverloads
    fun toArray(array: DoubleArray = DoubleArray(4), offset: Int = 0): DoubleArray {
        array[offset + 0] = _x
        array[offset + 1] = _y
        array[offset + 2] = _z
        array[offset + 3] = _w
        return array
    }

}
