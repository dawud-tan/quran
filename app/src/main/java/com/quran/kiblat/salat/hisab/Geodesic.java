package com.quran.kiblat.salat.hisab;

// This class implements the inverse geodesic problem specialized to returning a Qibla azimuth
// from a point in Indonesia to the Kaaba. Each constant and computation below corresponds to
// quantities in Karney's formulation (auxiliary sphere, reduced latitude, series coefficients, ε/k²,
// and the Newton solve for the longitude difference λ).
public class Geodesic {

    // Karney models the earth as an oblate ellipsoid with flattening F; this value is F = 1/298.257223563
    // (WGS84). Flattening appears throughout the formulas (in k² and ε, and in reduced-latitude transforms).
    private static final double F = 0.0033528106647474805d;           // F = WGS84 flattening

    // Karney frequently uses 1-F as part of the reduced-latitude and conversion between geodetic and
    // functions on the auxiliary sphere.
    private static final double ONE_MINUS_F = 0.9966471893352525d;   // 1−F   → used for reduced latitude tan β = (1−F) tan φ

    // KAABA longitude (target longitude)
    // The inverse problem in Karney takes two longitudes; here the target longitude is the Kaaba's.
    private static final double KAABA_LAMBDA2 = 39.8262d; // λ₂ fixed (Kaaba ≈ 39.8262°E)

    // SIN_BETA_2, COS_BETA_2 — precomputed reduced-latitude trig at Kaaba
    // Karney introduces the *reduced latitude* β (beta) via tan β = (1−F) tan φ, and many formulas
    // use sin β and cos β for the endpoint A (here the Kaaba). Precomputing sinβ and cosβ for the
    // fixed target simplifies later expressions.
    private static final double SIN_BETA_2 = -0.36418108372878055d; // sin β₂
    private static final double COS_BETA_2 = 0.931328158198887d; // cos β₂

    // Coefficients of the C₃(ε) polynomial series for longitude correction term
    // (related to I₃ integral expansion — Karney Eq. 23–25, but here using high-order fixed coeffs)
    private static final double[] C3_POLY_COEFFS = {
            0.0234375d, 0.03908873781853724d, 0.04695366939653196d, 0.12499964752736174d,
            0.24958019490340408d, 0.01953125d, 0.02345061890926862d, 0.046822392185686165d,
            0.062342661206936094d, 0.013671875d, 0.023393770302437927d, 0.025963026642854565d,
            0.013671875d, 0.01362595881755982d, 0.008203125d
    };

    // LAMBDA_POLY_COEFFS used in the λ correction term (see Karney's expression for the longitude difference
    // λ involving a polynomial in ε multiplied by sinα0 and series contributions). These coefficients
    // appear in the implementation of the correction to the spherical longitude on the auxiliary sphere.
    private static final double[] LAMBDA_POLY_COEFFS = {
            -0.0234375d, -0.046927475637074494d, -0.06281503005876607d,
            -0.2502088451303832d, -0.49916038980680816d, 1.0d
    };

    // A1_POLY_COEFFS: polynomial used to compute A1(ε) (the coefficient multiplying I1 in Karney's notation)
    // Karney defines A1 as a function of ε (or k²) using a rational combination — the code packs a small
    // polynomial to evaluate this efficiently.
    private static final double[] A1_POLY_COEFFS = {1.0d, 4.0d, 64.0d, 0.0d, 256.0d};

    // C1_POLY_COEFFS: packing to compute the C1 polynomial series (used in I1 expansion)
    // Karney gives explicit C1 series coefficients for the Fourier expansion; this array is an
    // intermediate packing used to generate the final C1_SERIES_COEFFICIENTS by Horner-like evaluation.
    private static final double[] C1_POLY_COEFFS = {
            -1.0d, 6.0d, -16.0d, 32.0d,
            -9.0d, 64.0d, -128.0d, 2048.0d,
            9.0d, -16.0d, 768.0d,
            3.0d, -5.0d, 512.0d,
            -7.0d, 1280.0d,
            -7.0d, 2048.0d
    };

    // Output arrays for the final series coefficients (C1 and C3) used in the Fourier-series sums
    // I1 and I3 (Karney's notation). Karney constructs C coefficients (C1, C2, C3) to evaluate the
    // sine/cosine series for integrals; these arrays will be populated from the packed polynomial data above.
    private static final double[] C1_SERIES_COEFFICIENTS = new double[7];
    private static final double[] C3_SERIES_COEFFICIENTS = new double[6];

    // determineIndonesianQiblaDirection implements the inverse geodesic specialized to Indonesia bounds.
    // The algorithm follows Karney's inverse problem: form reduced latitudes on the auxiliary sphere,
    // compute spherical quantities (σ, ω) and then apply series corrections (I1/I3 via C1/C3 and A1)
    // and finally solve for the λ that makes the longitudes match — Newton iteration on λ.
    public static double determineIndonesianQiblaDirection(double latitude1Phi1, double longitude1lambda1) {
        if (latitude1Phi1 < -11.0d || latitude1Phi1 > 6.1d || longitude1lambda1 < 95.0d || longitude1lambda1 > 142.0d) {
            return Double.NaN;
        }

        // lambda12 = longitudinal difference between location longitude/Lambda1 and Kaaba longitude/Lambda2 (degrees)
        // Karney works in radians; here the code keeps degrees and uses sinCos helper to get sin/cos.
        var lambda12 = longitude1lambda1 - KAABA_LAMBDA2;

        // sinCosLambda12 of the longitude difference (helper returns sin and cos of angle in degrees)
        // This corresponds to Karney's use of sin(λ12) and cos(λ12) when manipulating spherical trig on the auxiliary sphere.
        var sinCosLambda12 = sinCos(lambda12);

        // sinCosPhi1 of negative latitude (preparing reduced latitude computation)
        // Karney maps geodetic latitude φ to reduced latitude β using tan β = (1−F) tan φ.
        var sinCosPhi1 = sinCos(-latitude1Phi1);

        // Compute sin_beta1 and cos_beta1 for the *origin* after applying the (1 - F) factor — the reduced-latitude transform.
        // Karney repeatedly uses the reduced-latitude components: sin β = (1−F) sin φ / sqrt((1−F)² sin² φ + cos² φ)
        // and cos β = cos φ / sqrt((1−F)² sin² φ + cos² φ). This matches the auxiliary-sphere construction.
        var beta1_magnitude = Math.hypot(ONE_MINUS_F * sinCosPhi1.first, sinCosPhi1.second);
        var sin_beta1 = ONE_MINUS_F * sinCosPhi1.first / beta1_magnitude;
        var cos_beta1 = sinCosPhi1.second / beta1_magnitude;

        // sin_beta_diff: construct combination used to compute cosα1 (projection onto Kaaba reduced-latitude orientation)
        // This is Karney's algebra projecting reduced-latitude sines/cosines between the two points on the auxiliary sphere.
        var sin_beta_diff = sin_beta1 * COS_BETA_2 - cos_beta1 * SIN_BETA_2;

        // sin_alpha1: initial sine of the azimuth at the starting point (on the auxiliary sphere)
        // Karney defines α as the azimuth on the auxiliary sphere; sinα1 uses the reduced-latitude cos and the longitude difference.
        var sin_alpha1 = cos_beta1 * sinCosLambda12.first;

        // cos_alpha1: initial cosine of the azimuth at the starting point
        // This implements Karney's formula for cosα1 expressed in terms of the reduced-latitude components and the
        // spherical longitude difference; note the safe division by (1 + cos λ) for the numerical form he suggests.
        var cos_alpha1 = sin_beta_diff + cos_beta1 * SIN_BETA_2 * sinCosLambda12.first
                * sinCosLambda12.first / (1 + sinCosLambda12.second);

        // prepare variables that will be filled inside the iterative Newton loop
        var sin_alpha2 = Double.NaN;
        var cos_alpha2 = Double.NaN;
        var sin_alpha1_prev = 0.0d;
        var cos_alpha1_prev = -1.0d;

        // Newton iteration loop to solve for λ (the spherical longitude on the auxiliary sphere)
        // Karney solves the inverse problem by iterating on the auxiliary-sphere longitude difference λ,
        // using a Newton step computed from the derivative of the longitude with respect to α1.
        var converged = false;
        var lambda_residual = 0.0d;
        do {

            // sin_alpha0, cos_alpha0 — intermediate rotated auxiliary-sphere azimuth using Kaaba's reduced-latitude
            // Karney introduces α0 (azimuth on the equatorial sphere) via trigonometric identities mixing sinα and sinβ.
            var sin_alpha0 = sin_alpha1 * COS_BETA_2;
            var cos_alpha0 = Math.hypot(cos_alpha1, sin_alpha1 * SIN_BETA_2);

            // sin_omega1, cos_omega1 — spherical-circle trig components on the auxiliary sphere for point 1
            // Karney uses ω and σ variables for longitudes/arc angles on the auxiliary sphere. These compute
            // sin(ω1) and cos(ω1) in the algebraic form used in the series expansions.
            var sin_omega1 = sin_alpha0 * SIN_BETA_2;
            var cos_omega1 = cos_alpha1 * COS_BETA_2;

            // Normalize sigma1 direction cosines (sin_sigma1/cos_sigma1)
            // Karney constructs normalized sine/cosine of σ1 (the spherical arc distance) for use in the Fourier sums.
            var sigma1_magnitude = Math.hypot(SIN_BETA_2, cos_omega1);
            var sin_sigma1 = SIN_BETA_2 / sigma1_magnitude;
            var cos_sigma1 = cos_omega1 / sigma1_magnitude;

            // sin_alpha2, cos_alpha2 — azimuth components at the Kaaba (point 2) after mapping back from auxiliary sphere
            // Karney gives formulas to map between α0 and α2 (the azimuths on auxiliary/physical spheres). These lines implement that mapping.
            sin_alpha2 = sin_alpha0 / cos_beta1;
            cos_alpha2 = Math.sqrt(cos_alpha1 * COS_BETA_2 * cos_alpha1 * COS_BETA_2
                    + (SIN_BETA_2 - sin_beta1) * (SIN_BETA_2 + sin_beta1)) / cos_beta1;

            // sin_omega2, cos_omega2 — spherical-circle trig components on the auxiliary sphere for point 2
            var sin_omega2 = sin_alpha0 * sin_beta1;
            var cos_omega2 = cos_alpha2 * cos_beta1;

            // Normalize sigma2 (σ2) direction cosines
            var sigma2_magnitude = Math.hypot(sin_beta1, cos_omega2);
            var sin_sigma2 = sin_beta1 / sigma2_magnitude;
            var cos_sigma2 = cos_omega2 / sigma2_magnitude;

            // sigma12 (σ12) — spherical arc difference between σ1 and σ2 on the auxiliary sphere
            // Karney uses atan2 on combination of normalized sines/cosines to obtain the signed spherical separation σ12.
            var sigma12 = Math.atan2(cos_sigma1 * sin_sigma2 - sin_sigma1 * cos_sigma2,
                    cos_sigma1 * cos_sigma2 + sin_sigma1 * sin_sigma2);

            // sin_omega12, cos_omega12 — trig of the spherical long difference ω12
            // These appear in Karney's expression for the initial guess of λ and the correction
            var sin_omega12 = cos_omega1 * sin_omega2 - sin_omega1 * cos_omega2;
            var cos_omega12 = cos_omega1 * cos_omega2 + sin_omega1 * sin_omega2;

            // kSquared — Karney's k², see Sect. 6: k² = cos²α0 * F (2−F) / (1−F)²
            // This parameter measures eccentricity-modulated projection and is the small parameter for the series.
            // Karney then defines ε in terms of k² to accelerate series convergence.
            var kSquared = cos_alpha0 * cos_alpha0 * F * (2.0d - F)
                    / (ONE_MINUS_F * ONE_MINUS_F);

            // ε (epsilon) as Karney defines it: ε = k² / (2(1+√(1+k²)) + k²)
            // This reparametrization stabilizes the series (ε is small when k² is small). Karney uses ε for
            // constructing the A and C series (improves numerical behavior vs using k² directly).
            var epsilon = kSquared / (2.0d * (1.0d + Math.sqrt(1.0d + kSquared)) + kSquared);

            // Now build the C3 series coefficients (C3_SERIES_COEFFICIENTS) from the packed polynomial table.
            // Karney's implementation evaluates polynomials in ε to produce the Fourier coefficients for I3.
            // The code uses Horner-like evaluation and multiplies by the powers of ε (multiplier) as specified by Karney.
            var multiplier = 1.0d;
            var offset = 0;
            for (var degree = 1; degree < 6; degree++) {
                var remainingTerms = 6 - degree - 1;
                multiplier *= epsilon;
                //horner eval
                var c3Degree = remainingTerms;
                var c3StartIndex = offset;
                var y = C3_POLY_COEFFS[c3StartIndex++];
                while (--c3Degree >= 0) {
                    y = y * epsilon + C3_POLY_COEFFS[c3StartIndex++];
                }

                // This stores the final coefficient for the Fourier series term (degree) of I3.
                // Karney arranges these so that the sin-cos series evaluation is efficient and stable.
                C3_SERIES_COEFFICIENTS[degree] = multiplier * y;
                offset += remainingTerms + 1;
            }

            // Evaluate a polynomial in ε to get the λ correction multiplier yLambda
            // Karney shows that the correction to spherical longitude involves a polynomial in ε times sinα0
            // and the difference of the I3 series evaluated at the two σ positions. This step yields that polynomial value.
            var lambdaDegree = 5;
            var lambdaStartIndex = 0;
            var y_lambda = LAMBDA_POLY_COEFFS[lambdaStartIndex++];
            while (--lambdaDegree >= 0) {
                y_lambda = y_lambda * epsilon + LAMBDA_POLY_COEFFS[lambdaStartIndex++];
            }

            // lambda_residual — the discrepancy in λ implied by the current α1 estimate
            // Karney's formula: λ = atan2( ... spherical terms ...) − F * yLambda * sinα0 * (σ12 + I3(σ2) − I3(σ1))
            // This line computes the λ residual (signed) whose root we seek (we iterate to make λValue = 0).
            lambda_residual = Math.atan2(sin_omega12 * sinCosLambda12.second - cos_omega12 * sinCosLambda12.first,
                    cos_omega12 * sinCosLambda12.second + sin_omega12 * sinCosLambda12.first) - F * y_lambda * sin_alpha0
                    * (sigma12 + sinCosSeries(sin_sigma2, cos_sigma2, C3_SERIES_COEFFICIENTS)
                    - sinCosSeries(sin_sigma1, cos_sigma1, C3_SERIES_COEFFICIENTS));

            // Compute A1 from polynomial (Karney defines A1 as a function of ε; this code evaluates that rational form)
            // A1 multiplies the I1 series in the expression for the longitude derivative used in the Newton update.
            var a1Degree = 3;
            var a1StartIndex = 0;
            var y_a1 = A1_POLY_COEFFS[a1StartIndex++];
            while (--a1Degree >= 0) {
                y_a1 = y_a1 * epsilon * epsilon + A1_POLY_COEFFS[a1StartIndex++];
            }

            // Karney's A1 normalization: (yA1 / A1_POLY_COEFFS[4] + ε) / (1 − ε)
            var A1 = (y_a1 / A1_POLY_COEFFS[4] + epsilon) / (1 - epsilon);

            // Now compute the C1 series coefficients (used in the I1 integral series)
            // Karney supplies the algebra to compute C1 coefficients from ε using packed polynomial tables;
            // this block reconstructs those Fourier coefficients, again using Horner-like evaluation.
            var c1Offset = 0;
            for (var degree = 1; degree <= 6; degree++) {
                var remainingTerms = (6 - degree) / 2;
                var c1Degree = 3;
                var c1StartIndex = 0;
                var yC1 = C1_POLY_COEFFS[c1StartIndex++];
                while (--c1Degree >= 0) {
                    yC1 = yC1 * epsilon * epsilon + C1_POLY_COEFFS[c1StartIndex++];
                }

                // Karney's formula for C1 coefficient (note division by a packing index) — these yield the Fourier terms
                // used to evaluate I1 via the sin/cos series helper below.
                C1_SERIES_COEFFICIENTS[degree] = epsilon * yC1 / C1_POLY_COEFFS[c1Offset + remainingTerms + 1];
                c1Offset += remainingTerms + 2;
                epsilon *= epsilon;
            }

            // Increment A1 as Karney's definition requires for the derivative of λ with respect to α1
            A1++;

            // dlambda_dalpha1 — ∂λ/∂α1 as used in the Newton step.
            // Karney gives an explicit expression for the derivative (see Sect. 8) involving a1 and the I1 series difference.
            // This is the key factor in the Newton update Δα1 = −λ / (∂λ/∂α1).
            var dlambda_dalpha1 = A1 * (sigma12
                    + sinCosSeries(sin_sigma2, cos_sigma2, C1_SERIES_COEFFICIENTS)
                    - sinCosSeries(sin_sigma1, cos_sigma1, C1_SERIES_COEFFICIENTS));
            dlambda_dalpha1 *= ONE_MINUS_F / (cos_alpha2 * cos_beta1);

            // Convergence check/Termination test: if the λ residual is effectively zero (within floating point unit),
            // the Newton iteration has converged. Karney discusses numerical tolerances and stopping criteria.
            converged = Math.abs(lambda_residual) < Math.ulp(1.0d);

            if (!converged) {
                // Bookkeeping for safeguarded Newton (tracking best bracket when crossing discontinuity) — not a direct quote from Karney but
                // a common safe-Newton practice: if λ has same sign and magnitude increases, update bracket.
                if (lambda_residual > 0.0d && cos_alpha1 / sin_alpha1 > cos_alpha1_prev / sin_alpha1_prev) {
                    sin_alpha1_prev = sin_alpha1;
                    cos_alpha1_prev = cos_alpha1;
                }

                // Newton step: Δα1 = −λ / (∂λ/∂α1)
                // Karney uses this linearization (and discusses robust step control) to update α1 until λ = 0.
                var delta_alpha1 = -lambda_residual / dlambda_dalpha1;
                var sinDeltaAlpha1 = Math.sin(delta_alpha1);
                var cosDeltaAlpha1 = Math.cos(delta_alpha1);

                // Rotate (sinAlpha1, cosAlpha1) by Δα1 (updating the azimuth estimate)
                // This is numerically better than recomputing α1 from arctan each iteration.
                var new_cos = cos_alpha1 * cosDeltaAlpha1 - sin_alpha1 * sinDeltaAlpha1;
                var new_sin = sin_alpha1 * cosDeltaAlpha1 + cos_alpha1 * sinDeltaAlpha1;

                // Normalize back to unit vector representation for numerical stability (Karney stresses stable trig).
                var alpha1_magnitude = Math.hypot(new_sin, new_cos);
                sin_alpha1 = new_sin / alpha1_magnitude;
                cos_alpha1 = new_cos / alpha1_magnitude;
            }
        } while (!converged);

        // After convergence swap cosAlpha2/sinAlpha2 — this code arranges the values to compute
        // the final azimuth, swap sin/cos because atan2(y,x) convention.
        var temp = cos_alpha2;
        cos_alpha2 = sin_alpha2;
        sin_alpha2 = temp;

        // Return the azimuth in degrees from North (this code maps Karney's α2 to a bearing).
        // Karney provides the final mapping from the auxiliary-sphere azimuth back to geodetic azimuth.
        return 270.0d + Math.atan2(sin_alpha2, cos_alpha2) * 180.0d / Math.PI;
    }

    // sinCos helper: computes sine and cosine of a degree angle using quadrant reduction.
    // Karney's formulas are in radians, but the trig identities are the same — this helper keeps degrees
    // to match the rest of the code. Note: Karney emphasizes careful trig handling to avoid cancellation.
    private static Pair sinCos(final double degrees) {
        var degreesMod = degrees % 360.0d;
        var quadrant = (int) Math.round(degreesMod / 90.0d);
        degreesMod -= 90.0d * quadrant;
        var radians = degreesMod * Math.PI / 180.0;
        var sinResult = (quadrant & 3) == 0 ? Math.sin(radians) : (quadrant & 3) == 1 ? Math.cos(radians) : 0;
        var cosResult = (quadrant & 3) == 0 ? Math.cos(radians) : (quadrant & 3) == 1 ? -Math.sin(radians) : 0;
        sinResult = sinResult == 0 ? Math.copySign(sinResult, degrees) : sinResult;
        return new Pair(sinResult, cosResult);
    }

    // sinCosSeries evaluates the Fourier-type series Karney uses to express the integrals I1 and I3.
    // The routine implements an efficient recurrence (Clenshaw/Horner-like) to evaluate the sum of cos/sin terms
    // of the form 2 sin σ cos σ * (∑ coefficients * T^n ...) that Karney uses in his expansions.
    // See Karney's Section on Fourier-series evaluation for I1 and I3; this implementation mirrors the
    // compact recurrence he presents for numerical stability and speed.
    private static double sinCosSeries(final double sinValue, final double cosValue, final double[] coefficients) {
        var index = coefficients.length;
        var order = index - 1;
        var ar = 2.0d * (cosValue - sinValue) * (cosValue + sinValue);
        var evenTerm = (order & 1) != 0 ? coefficients[--index] : 0.0d;
        var oddTerm = 0.0d;
        order /= 2;
        while (order-- > 0) {
            oddTerm = ar * evenTerm - oddTerm + coefficients[--index];
            evenTerm = ar * oddTerm - evenTerm + coefficients[--index];
        }
        return 2.0d * sinValue * cosValue * evenTerm;
    }

    // Simple pair record used as a tiny tuple for sin/cos return
    record Pair(double first, double second) {

    }
}