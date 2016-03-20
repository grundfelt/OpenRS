/* OpenRoadSynth - The free road noise synthisizer

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package se.grundfelt.openroadsynth;

/**
 *
 * @author from org.apache.commons.math3.complex.
 */
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;

public class Complex{

    public static final Complex I = new Complex(0.0D, 1.0D);
    public static final Complex NaN = new Complex((0.0D / 0.0D), (0.0D / 0.0D));
    public static final Complex INF = new Complex((1.0D / 0.0D), (1.0D / 0.0D));
    public static final Complex ONE = new Complex(1.0D, 0.0D);
    public static final Complex ZERO = new Complex(0.0D, 0.0D);
    private static final long serialVersionUID = -6195664516687396620L;
    private final double imaginary;
    private final double real;
    private final transient boolean isNaN;
    private final transient boolean isInfinite;

    public Complex(double real) {
        this(real, 0.0D);
    }

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;

        this.isNaN = ((Double.isNaN(real)) || (Double.isNaN(imaginary)));
        this.isInfinite = ((!this.isNaN) && ((Double.isInfinite(real)) || (Double.isInfinite(imaginary))));
    }

    public double abs() {
        if (this.isNaN) {
            return (0.0D / 0.0D);
        }
        if (isInfinite()) {
            return (1.0D / 0.0D);
        }
        if (FastMath.abs(this.real) < FastMath.abs(this.imaginary)) {
            if (this.imaginary == 0.0D) {
                return FastMath.abs(this.real);
            }
            double q = this.real / this.imaginary;
            return FastMath.abs(this.imaginary) * FastMath.sqrt(1.0D + q * q);
        }
        if (this.real == 0.0D) {
            return FastMath.abs(this.imaginary);
        }
        double q = this.imaginary / this.real;
        return FastMath.abs(this.real) * FastMath.sqrt(1.0D + q * q);
    }

    public Complex add(Complex addend)
            throws NullArgumentException {
        MathUtils.checkNotNull(addend);
        if ((this.isNaN) || (addend.isNaN)) {
            return NaN;
        }
        return createComplex(this.real + addend.getReal(), this.imaginary + addend.getImaginary());
    }

    public Complex add(double addend) {
        if ((this.isNaN) || (Double.isNaN(addend))) {
            return NaN;
        }
        return createComplex(this.real + addend, this.imaginary);
    }

    public Complex conjugate() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(this.real, -this.imaginary);
    }

    public Complex divide(Complex divisor)
            throws NullArgumentException {
        MathUtils.checkNotNull(divisor);
        if ((this.isNaN) || (divisor.isNaN)) {
            return NaN;
        }
        double c = divisor.getReal();
        double d = divisor.getImaginary();
        if ((c == 0.0D) && (d == 0.0D)) {
            return NaN;
        }
        if ((divisor.isInfinite()) && (!isInfinite())) {
            return ZERO;
        }
        if (FastMath.abs(c) < FastMath.abs(d)) {
            double q = c / d;
            double denominator = c * q + d;
            return createComplex((this.real * q + this.imaginary) / denominator, (this.imaginary * q - this.real) / denominator);
        }
        double q = d / c;
        double denominator = d * q + c;
        return createComplex((this.imaginary * q + this.real) / denominator, (this.imaginary - this.real * q) / denominator);
    }

    public Complex divide(double divisor) {
        if ((this.isNaN) || (Double.isNaN(divisor))) {
            return NaN;
        }
        if (divisor == 0.0D) {
            return NaN;
        }
        if (Double.isInfinite(divisor)) {
            return !isInfinite() ? ZERO : NaN;
        }
        return createComplex(this.real / divisor, this.imaginary / divisor);
    }

    public Complex reciprocal() {
        if (this.isNaN) {
            return NaN;
        }
        if ((this.real == 0.0D) && (this.imaginary == 0.0D)) {
            return INF;
        }
        if (this.isInfinite) {
            return ZERO;
        }
        if (FastMath.abs(this.real) < FastMath.abs(this.imaginary)) {
            double q = this.real / this.imaginary;
            double scale = 1.0D / (this.real * q + this.imaginary);
            return createComplex(scale * q, -scale);
        }
        double q = this.imaginary / this.real;
        double scale = 1.0D / (this.imaginary * q + this.real);
        return createComplex(scale, -scale * q);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if ((other instanceof Complex)) {
            Complex c = (Complex) other;
            if (c.isNaN) {
                return this.isNaN;
            }
            return (MathUtils.equals(this.real, c.real)) && (MathUtils.equals(this.imaginary, c.imaginary));
        }
        return false;
    }

    public static boolean equals(Complex x, Complex y, int maxUlps) {
        return (Precision.equals(x.real, y.real, maxUlps)) && (Precision.equals(x.imaginary, y.imaginary, maxUlps));
    }

    public static boolean equals(Complex x, Complex y) {
        return equals(x, y, 1);
    }

    public static boolean equals(Complex x, Complex y, double eps) {
        return (Precision.equals(x.real, y.real, eps)) && (Precision.equals(x.imaginary, y.imaginary, eps));
    }

    public static boolean equalsWithRelativeTolerance(Complex x, Complex y, double eps) {
        return (Precision.equalsWithRelativeTolerance(x.real, y.real, eps)) && (Precision.equalsWithRelativeTolerance(x.imaginary, y.imaginary, eps));
    }

    public int hashCode() {
        if (this.isNaN) {
            return 7;
        }
        return 37 * (17 * MathUtils.hash(this.imaginary) + MathUtils.hash(this.real));
    }

    public double getImaginary() {
        return this.imaginary;
    }

    public double getReal() {
        return this.real;
    }

    public boolean isNaN() {
        return this.isNaN;
    }

    public boolean isInfinite() {
        return this.isInfinite;
    }

    public Complex multiply(Complex factor)
            throws NullArgumentException {
        MathUtils.checkNotNull(factor);
        if ((this.isNaN) || (factor.isNaN)) {
            return NaN;
        }
        if ((Double.isInfinite(this.real)) || (Double.isInfinite(this.imaginary)) || (Double.isInfinite(factor.real)) || (Double.isInfinite(factor.imaginary))) {
            return INF;
        }
        return createComplex(this.real * factor.real - this.imaginary * factor.imaginary, this.real * factor.imaginary + this.imaginary * factor.real);
    }

    public Complex multiply(int factor) {
        if (this.isNaN) {
            return NaN;
        }
        if ((Double.isInfinite(this.real)) || (Double.isInfinite(this.imaginary))) {
            return INF;
        }
        return createComplex(this.real * factor, this.imaginary * factor);
    }

    public Complex multiply(double factor) {
        if ((this.isNaN) || (Double.isNaN(factor))) {
            return NaN;
        }
        if ((Double.isInfinite(this.real)) || (Double.isInfinite(this.imaginary)) || (Double.isInfinite(factor))) {
            return INF;
        }
        return createComplex(this.real * factor, this.imaginary * factor);
    }

    public Complex negate() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(-this.real, -this.imaginary);
    }

    public Complex subtract(Complex subtrahend)
            throws NullArgumentException {
        MathUtils.checkNotNull(subtrahend);
        if ((this.isNaN) || (subtrahend.isNaN)) {
            return NaN;
        }
        return createComplex(this.real - subtrahend.getReal(), this.imaginary - subtrahend.getImaginary());
    }

    public Complex subtract(double subtrahend) {
        if ((this.isNaN) || (Double.isNaN(subtrahend))) {
            return NaN;
        }
        return createComplex(this.real - subtrahend, this.imaginary);
    }

    public Complex acos() {
        if (this.isNaN) {
            return NaN;
        }
        return add(sqrt1z().multiply(I)).log().multiply(I.negate());
    }

    public Complex asin() {
        if (this.isNaN) {
            return NaN;
        }
        return sqrt1z().add(multiply(I)).log().multiply(I.negate());
    }

    public Complex atan() {
        if (this.isNaN) {
            return NaN;
        }
        return add(I).divide(I.subtract(this)).log().multiply(I.divide(createComplex(2.0D, 0.0D)));
    }

    public Complex cos() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(FastMath.cos(this.real) * FastMath.cosh(this.imaginary), -FastMath.sin(this.real) * FastMath.sinh(this.imaginary));
    }

    public Complex cosh() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(FastMath.cosh(this.real) * FastMath.cos(this.imaginary), FastMath.sinh(this.real) * FastMath.sin(this.imaginary));
    }

    public Complex exp() {
        if (this.isNaN) {
            return NaN;
        }
        double expReal = FastMath.exp(this.real);
        return createComplex(expReal * FastMath.cos(this.imaginary), expReal * FastMath.sin(this.imaginary));
    }

    public Complex log() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(FastMath.log(abs()), FastMath.atan2(this.imaginary, this.real));
    }

    public Complex pow(Complex x)
            throws NullArgumentException {
        MathUtils.checkNotNull(x);
        return log().multiply(x).exp();
    }

    public Complex pow(double x) {
        return log().multiply(x).exp();
    }

    public Complex sin() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(FastMath.sin(this.real) * FastMath.cosh(this.imaginary), FastMath.cos(this.real) * FastMath.sinh(this.imaginary));
    }

    public Complex sinh() {
        if (this.isNaN) {
            return NaN;
        }
        return createComplex(FastMath.sinh(this.real) * FastMath.cos(this.imaginary), FastMath.cosh(this.real) * FastMath.sin(this.imaginary));
    }

    public Complex sqrt() {
        if (this.isNaN) {
            return NaN;
        }
        if ((this.real == 0.0D) && (this.imaginary == 0.0D)) {
            return createComplex(0.0D, 0.0D);
        }
        double t = FastMath.sqrt((FastMath.abs(this.real) + abs()) / 2.0D);
        if (this.real >= 0.0D) {
            return createComplex(t, this.imaginary / (2.0D * t));
        }
        return createComplex(FastMath.abs(this.imaginary) / (2.0D * t), FastMath.copySign(1.0D, this.imaginary) * t);
    }

    public Complex sqrt1z() {
        return createComplex(1.0D, 0.0D).subtract(multiply(this)).sqrt();
    }

    public Complex tan() {
        if ((this.isNaN) || (Double.isInfinite(this.real))) {
            return NaN;
        }
        if (this.imaginary > 20.0D) {
            return createComplex(0.0D, 1.0D);
        }
        if (this.imaginary < -20.0D) {
            return createComplex(0.0D, -1.0D);
        }
        double real2 = 2.0D * this.real;
        double imaginary2 = 2.0D * this.imaginary;
        double d = FastMath.cos(real2) + FastMath.cosh(imaginary2);

        return createComplex(FastMath.sin(real2) / d, FastMath.sinh(imaginary2) / d);
    }

    public Complex tanh() {
        if ((this.isNaN) || (Double.isInfinite(this.imaginary))) {
            return NaN;
        }
        if (this.real > 20.0D) {
            return createComplex(1.0D, 0.0D);
        }
        if (this.real < -20.0D) {
            return createComplex(-1.0D, 0.0D);
        }
        double real2 = 2.0D * this.real;
        double imaginary2 = 2.0D * this.imaginary;
        double d = FastMath.cosh(real2) + FastMath.cos(imaginary2);

        return createComplex(FastMath.sinh(real2) / d, FastMath.sin(imaginary2) / d);
    }
    
    public Complex coth() {
        Complex nom = this.cosh();
        Complex den = this.sinh();
        return nom.divide(den);
    }

    public double getArgument() {
        return FastMath.atan2(getImaginary(), getReal());
    }

    public List<Complex> nthRoot(int n)
            throws NotPositiveException {
        if (n <= 0) {
            throw new NotPositiveException(LocalizedFormats.CANNOT_COMPUTE_NTH_ROOT_FOR_NEGATIVE_N, Integer.valueOf(n));
        }
        List<Complex> result = new ArrayList();
        if (this.isNaN) {
            result.add(NaN);
            return result;
        }
        if (isInfinite()) {
            result.add(INF);
            return result;
        }
        double nthRootOfAbs = FastMath.pow(abs(), 1.0D / n);

        double nthPhi = getArgument() / n;
        double slice = 6.283185307179586D / n;
        double innerPart = nthPhi;
        for (int k = 0; k < n; k++) {
            double realPart = nthRootOfAbs * FastMath.cos(innerPart);
            double imaginaryPart = nthRootOfAbs * FastMath.sin(innerPart);
            result.add(createComplex(realPart, imaginaryPart));
            innerPart += slice;
        }
        return result;
    }

    protected Complex createComplex(double realPart, double imaginaryPart) {
        return new Complex(realPart, imaginaryPart);
    }

    public static Complex valueOf(double realPart, double imaginaryPart) {
        if ((Double.isNaN(realPart)) || (Double.isNaN(imaginaryPart))) {
            return NaN;
        }
        return new Complex(realPart, imaginaryPart);
    }

    public static Complex valueOf(double realPart) {
        if (Double.isNaN(realPart)) {
            return NaN;
        }
        return new Complex(realPart);
    }

    protected final Object readResolve() {
        return createComplex(this.real, this.imaginary);
    }

    public String toString() {
        return "(" + this.real + ", " + this.imaginary + ")";
    }
}
