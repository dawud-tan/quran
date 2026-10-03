package com.quran.kiblat.salat.ui.util;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dawud_tan
 */
public class SolarPosition {

    // Difference between earth rotation time (UT1) and terrestrial time (TT), in seconds.
    // Nilai pengamatan IERS untuk 2020-2026 stabil di sekitar 69,2 detik.
    // Pengaruhnya ke jadwal salat < 0,1 detik, jadi cukup satu nilai untuk seluruh aplikasi.
    public static final double DELTA_T = 69.2;
    static final double SUN_RADIUS = 0.266666666666667;
    static final int L_COUNT = 6;
    static final int B_COUNT = 2;
    static final int R_COUNT = 5;
    static final int Y_COUNT = 63;
    static final int[] l_subcount = {64, 34, 20, 7, 3, 1};
    /// ////////////////////////////////////////////////
    ///  Earth Periodic Terms
    /// ////////////////////////////////////////////////
    static final double[][][] SUKU2_L = new double[][][]{
            {
                    {175347046.0, 0, 0},
                    {3341656.0, 4.6692568, 6283.07585},
                    {34894.0, 4.6261, 12566.1517},
                    {3497.0, 2.7441, 5753.3849},
                    {3418.0, 2.8289, 3.5231},
                    {3136.0, 3.6277, 77713.7715},
                    {2676.0, 4.4181, 7860.4194},
                    {2343.0, 6.1352, 3930.2097},
                    {1324.0, 0.7425, 11506.7698},
                    {1273.0, 2.0371, 529.691},
                    {1199.0, 1.1096, 1577.3435},
                    {990, 5.233, 5884.927},
                    {902, 2.045, 26.298},
                    {857, 3.508, 398.149},
                    {780, 1.179, 5223.694},
                    {753, 2.533, 5507.553},
                    {505, 4.583, 18849.228},
                    {492, 4.205, 775.523},
                    {357, 2.92, 0.067},
                    {317, 5.849, 11790.629},
                    {284, 1.899, 796.298},
                    {271, 0.315, 10977.079},
                    {243, 0.345, 5486.778},
                    {206, 4.806, 2544.314},
                    {205, 1.869, 5573.143},
                    {202, 2.458, 6069.777},
                    {156, 0.833, 213.299},
                    {132, 3.411, 2942.463},
                    {126, 1.083, 20.775},
                    {115, 0.645, 0.98},
                    {103, 0.636, 4694.003},
                    {102, 0.976, 15720.839},
                    {102, 4.267, 7.114},
                    {99, 6.21, 2146.17},
                    {98, 0.68, 155.42},
                    {86, 5.98, 161000.69},
                    {85, 1.3, 6275.96},
                    {85, 3.67, 71430.7},
                    {80, 1.81, 17260.15},
                    {79, 3.04, 12036.46},
                    {75, 1.76, 5088.63},
                    {74, 3.5, 3154.69},
                    {74, 4.68, 801.82},
                    {70, 0.83, 9437.76},
                    {62, 3.98, 8827.39},
                    {61, 1.82, 7084.9},
                    {57, 2.78, 6286.6},
                    {56, 4.39, 14143.5},
                    {56, 3.47, 6279.55},
                    {52, 0.19, 12139.55},
                    {52, 1.33, 1748.02},
                    {51, 0.28, 5856.48},
                    {49, 0.49, 1194.45},
                    {41, 5.37, 8429.24},
                    {41, 2.4, 19651.05},
                    {39, 6.17, 10447.39},
                    {37, 6.04, 10213.29},
                    {37, 2.57, 1059.38},
                    {36, 1.71, 2352.87},
                    {36, 1.78, 6812.77},
                    {33, 0.59, 17789.85},
                    {30, 0.44, 83996.85},
                    {30, 2.74, 1349.87},
                    {25, 3.16, 4690.48}
            },
            {
                    {628331966747.0, 0, 0},
                    {206059.0, 2.678235, 6283.07585},
                    {4303.0, 2.6351, 12566.1517},
                    {425.0, 1.59, 3.523},
                    {119.0, 5.796, 26.298},
                    {109.0, 2.966, 1577.344},
                    {93, 2.59, 18849.23},
                    {72, 1.14, 529.69},
                    {68, 1.87, 398.15},
                    {67, 4.41, 5507.55},
                    {59, 2.89, 5223.69},
                    {56, 2.17, 155.42},
                    {45, 0.4, 796.3},
                    {36, 0.47, 775.52},
                    {29, 2.65, 7.11},
                    {21, 5.34, 0.98},
                    {19, 1.85, 5486.78},
                    {19, 4.97, 213.3},
                    {17, 2.99, 6275.96},
                    {16, 0.03, 2544.31},
                    {16, 1.43, 2146.17},
                    {15, 1.21, 10977.08},
                    {12, 2.83, 1748.02},
                    {12, 3.26, 5088.63},
                    {12, 5.27, 1194.45},
                    {12, 2.08, 4694},
                    {11, 0.77, 553.57},
                    {10, 1.3, 6286.6},
                    {10, 4.24, 1349.87},
                    {9, 2.7, 242.73},
                    {9, 5.64, 951.72},
                    {8, 5.3, 2352.87},
                    {6, 2.65, 9437.76},
                    {6, 4.67, 4690.48}
            },
            {
                    {52919.0, 0, 0},
                    {8720.0, 1.0721, 6283.0758},
                    {309.0, 0.867, 12566.152},
                    {27, 0.05, 3.52},
                    {16, 5.19, 26.3},
                    {16, 3.68, 155.42},
                    {10, 0.76, 18849.23},
                    {9, 2.06, 77713.77},
                    {7, 0.83, 775.52},
                    {5, 4.66, 1577.34},
                    {4, 1.03, 7.11},
                    {4, 3.44, 5573.14},
                    {3, 5.14, 796.3},
                    {3, 6.05, 5507.55},
                    {3, 1.19, 242.73},
                    {3, 6.12, 529.69},
                    {3, 0.31, 398.15},
                    {3, 2.28, 553.57},
                    {2, 4.38, 5223.69},
                    {2, 3.75, 0.98}
            },
            {
                    {289.0, 5.844, 6283.076},
                    {35, 0, 0},
                    {17, 5.49, 12566.15},
                    {3, 5.2, 155.42},
                    {1, 4.72, 3.52},
                    {1, 5.3, 18849.23},
                    {1, 5.97, 242.73}
            },
            {
                    {114.0, 3.142, 0},
                    {8, 4.13, 6283.08},
                    {1, 3.84, 12566.15}
            },
            {
                    {1, 3.14, 0}
            }
    };
    static int[] b_subcount = {5, 2};
    static int[] r_subcount = {40, 10, 6, 2, 1};
    static double[][][] SUKU2_B
            = {
            {
                    {280.0, 3.199, 84334.662},
                    {102.0, 5.422, 5507.553},
                    {80, 3.88, 5223.69},
                    {44, 3.7, 2352.87},
                    {32, 4, 1577.34}
            },
            {
                    {9, 3.9, 5507.55},
                    {6, 1.73, 5223.69}
            }
    };
    static double[][][] SUKU2_R
            = {
            {
                    {100013989.0, 0, 0},
                    {1670700.0, 3.0984635, 6283.07585},
                    {13956.0, 3.05525, 12566.1517},
                    {3084.0, 5.1985, 77713.7715},
                    {1628.0, 1.1739, 5753.3849},
                    {1576.0, 2.8469, 7860.4194},
                    {925.0, 5.453, 11506.77},
                    {542.0, 4.564, 3930.21},
                    {472.0, 3.661, 5884.927},
                    {346.0, 0.964, 5507.553},
                    {329.0, 5.9, 5223.694},
                    {307.0, 0.299, 5573.143},
                    {243.0, 4.273, 11790.629},
                    {212.0, 5.847, 1577.344},
                    {186.0, 5.022, 10977.079},
                    {175.0, 3.012, 18849.228},
                    {110.0, 5.055, 5486.778},
                    {98, 0.89, 6069.78},
                    {86, 5.69, 15720.84},
                    {86, 1.27, 161000.69},
                    {65, 0.27, 17260.15},
                    {63, 0.92, 529.69},
                    {57, 2.01, 83996.85},
                    {56, 5.24, 71430.7},
                    {49, 3.25, 2544.31},
                    {47, 2.58, 775.52},
                    {45, 5.54, 9437.76},
                    {43, 6.01, 6275.96},
                    {39, 5.36, 4694},
                    {38, 2.39, 8827.39},
                    {37, 0.83, 19651.05},
                    {37, 4.9, 12139.55},
                    {36, 1.67, 12036.46},
                    {35, 1.84, 2942.46},
                    {33, 0.24, 7084.9},
                    {32, 0.18, 5088.63},
                    {32, 1.78, 398.15},
                    {28, 1.21, 6286.6},
                    {28, 1.9, 6279.55},
                    {26, 4.59, 10447.39}
            },
            {
                    {103019.0, 1.10749, 6283.07585},
                    {1721.0, 1.0644, 12566.1517},
                    {702.0, 3.142, 0},
                    {32, 1.02, 18849.23},
                    {31, 2.84, 5507.55},
                    {25, 1.32, 5223.69},
                    {18, 1.42, 1577.34},
                    {10, 5.91, 10977.08},
                    {9, 1.42, 6275.96},
                    {9, 0.27, 5486.78}
            },
            {
                    {4359.0, 5.7846, 6283.0758},
                    {124.0, 5.579, 12566.152},
                    {12, 3.14, 0},
                    {9, 3.63, 77713.77},
                    {6, 1.87, 5573.14},
                    {3, 5.47, 18849.23}
            },
            {
                    {145.0, 4.273, 6283.076},
                    {7, 3.92, 12566.15}
            },
            {
                    {4, 2.56, 6283.08}
            }
    };
    /// /////////////////////////////////////////////////////////////
    ///  Periodic Terms for the nutation in longitude and obliquity
    /// /////////////////////////////////////////////////////////////
    static int[][] SUKU2_Y
            = {
            {0, 0, 0, 0, 1},
            {-2, 0, 0, 2, 2},
            {0, 0, 0, 2, 2},
            {0, 0, 0, 0, 2},
            {0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {-2, 1, 0, 2, 2},
            {0, 0, 0, 2, 1},
            {0, 0, 1, 2, 2},
            {-2, -1, 0, 2, 2},
            {-2, 0, 1, 0, 0},
            {-2, 0, 0, 2, 1},
            {0, 0, -1, 2, 2},
            {2, 0, 0, 0, 0},
            {0, 0, 1, 0, 1},
            {2, 0, -1, 2, 2},
            {0, 0, -1, 0, 1},
            {0, 0, 1, 2, 1},
            {-2, 0, 2, 0, 0},
            {0, 0, -2, 2, 1},
            {2, 0, 0, 2, 2},
            {0, 0, 2, 2, 2},
            {0, 0, 2, 0, 0},
            {-2, 0, 1, 2, 2},
            {0, 0, 0, 2, 0},
            {-2, 0, 0, 2, 0},
            {0, 0, -1, 2, 1},
            {0, 2, 0, 0, 0},
            {2, 0, -1, 0, 1},
            {-2, 2, 0, 2, 2},
            {0, 1, 0, 0, 1},
            {-2, 0, 1, 0, 1},
            {0, -1, 0, 0, 1},
            {0, 0, 2, -2, 0},
            {2, 0, -1, 2, 1},
            {2, 0, 1, 2, 2},
            {0, 1, 0, 2, 2},
            {-2, 1, 1, 0, 0},
            {0, -1, 0, 2, 2},
            {2, 0, 0, 2, 1},
            {2, 0, 1, 0, 0},
            {-2, 0, 2, 2, 2},
            {-2, 0, 1, 2, 1},
            {2, 0, -2, 0, 1},
            {2, 0, 0, 0, 1},
            {0, -1, 1, 0, 0},
            {-2, -1, 0, 2, 1},
            {-2, 0, 0, 0, 1},
            {0, 0, 2, 2, 1},
            {-2, 0, 2, 0, 1},
            {-2, 1, 0, 2, 1},
            {0, 0, 1, -2, 0},
            {-1, 0, 1, 0, 0},
            {-2, 1, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {0, 0, 1, 2, 0},
            {0, 0, -2, 2, 2},
            {-1, -1, 1, 0, 0},
            {0, 1, 1, 0, 0},
            {0, -1, 1, 2, 2},
            {2, -1, -1, 2, 2},
            {0, 0, 3, 2, 2},
            {2, -1, 0, 2, 2},};
    static double[][] SUKU2_PE = {
            {-171996, -174.2, 92025, 8.9},
            {-13187, -1.6, 5736, -3.1},
            {-2274, -0.2, 977, -0.5},
            {2062, 0.2, -895, 0.5},
            {1426, -3.4, 54, -0.1},
            {712, 0.1, -7, 0},
            {-517, 1.2, 224, -0.6},
            {-386, -0.4, 200, 0},
            {-301, 0, 129, -0.1},
            {217, -0.5, -95, 0.3},
            {-158, 0, 0, 0},
            {129, 0.1, -70, 0},
            {123, 0, -53, 0},
            {63, 0, 0, 0},
            {63, 0.1, -33, 0},
            {-59, 0, 26, 0},
            {-58, -0.1, 32, 0},
            {-51, 0, 27, 0},
            {48, 0, 0, 0},
            {46, 0, -24, 0},
            {-38, 0, 16, 0},
            {-31, 0, 13, 0},
            {29, 0, 0, 0},
            {29, 0, -12, 0},
            {26, 0, 0, 0},
            {-22, 0, 0, 0},
            {21, 0, -10, 0},
            {17, -0.1, 0, 0},
            {16, 0, -8, 0},
            {-16, 0.1, 7, 0},
            {-15, 0, 9, 0},
            {-13, 0, 7, 0},
            {-12, 0, 6, 0},
            {11, 0, 0, 0},
            {-10, 0, 5, 0},
            {-8, 0, 3, 0},
            {7, 0, -3, 0},
            {-7, 0, 0, 0},
            {-7, 0, 3, 0},
            {-7, 0, 3, 0},
            {6, 0, 0, 0},
            {6, 0, -3, 0},
            {6, 0, -3, 0},
            {-6, 0, 3, 0},
            {-6, 0, 3, 0},
            {5, 0, 0, 0},
            {-5, 0, 3, 0},
            {-5, 0, 3, 0},
            {-5, 0, 3, 0},
            {4, 0, 0, 0},
            {4, 0, 0, 0},
            {4, 0, 0, 0},
            {-4, 0, 0, 0},
            {-4, 0, 0, 0},
            {-4, 0, 0, 0},
            {3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},
            {-3, 0, 0, 0},};

    public static double limitDegrees(double degrees) {
        double limited;

        degrees /= 360.0;
        limited = 360.0 * (degrees - Math.floor(degrees));
        if (limited < 0) {
            limited += 360.0;
        }

        return limited;
    }

    public static double limitDegrees180pm(double degrees) {
        double limited;

        degrees /= 360.0;
        limited = 360.0 * (degrees - Math.floor(degrees));
        if (limited < -180.0) {
            limited += 360.0;
        } else if (limited > 180.0) {
            limited -= 360.0;
        }

        return limited;
    }

    public static double limitDegrees180(double degrees) {
        double limited;

        degrees /= 180.0;
        limited = 180.0 * (degrees - Math.floor(degrees));
        if (limited < 0) {
            limited += 180.0;
        }

        return limited;
    }

    public static double limitZero2one(double value) {
        double limited;

        limited = value - Math.floor(value);
        if (limited < 0) {
            limited += 1.0;
        }

        return limited;
    }

    public static double limitMinutes(double minutes) {
        double limited = minutes;

        if (limited < -20.0) {
            limited += 1440.0;
        } else if (limited > 20.0) {
            limited -= 1440.0;
        }

        return limited;
    }

    public static double dayfracToLocalHr(double dayfrac, double timezone) {
        return 24.0 * limitZero2one(dayfrac + timezone / 24.0);
    }

    public static double thirdOrderPolynomial(double a, double b, double c, double d, double x) {
        return ((a * x + b) * x + c) * x + d;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    public static int validateInputs(SPAData spa) {
        if (spa.year < -2000 || spa.year > 6000) {
            return 1;
        }
        if (spa.month < 1 || spa.month > 12) {
            return 2;
        }
        if (spa.day < 1 || spa.day > 31) {
            return 3;
        }
        if (spa.hour < 0 || spa.hour > 24) {
            return 4;
        }
        if (spa.minute < 0 || spa.minute > 59) {
            return 5;
        }
        if (spa.second < 0 || spa.second >= 60) {
            return 6;
        }
        if (spa.pressure < 0 || spa.pressure > 5000) {
            return 12;
        }
        if (spa.temperature <= -273 || spa.temperature > 6000) {
            return 13;
        }
        if (spa.delta_ut1 < -1 || spa.delta_ut1 >= 1) {
            return 17;
        }
        if (spa.hour == 24 && spa.minute > 0) {
            return 5;
        }
        if (spa.hour == 24 && spa.second > 0) {
            return 6;
        }

        if (Math.abs(spa.delta_t) > 8000) {
            return 7;
        }
        if (Math.abs(spa.timezone) > 18) {
            return 8;
        }
        if (Math.abs(spa.longitude) > 180) {
            return 9;
        }
        if (Math.abs(spa.latitude) > 90) {
            return 10;
        }
        if (Math.abs(spa.atmos_refract) > 5) {
            return 16;
        }
        if (spa.elevation < -6500000) {
            return 11;
        }

        if (spa.function == SPA.SPA_ZA_INC || spa.function == SPA.SPA_ALL) {
            if (Math.abs(spa.slope) > 360) {
                return 14;
            }
            if (Math.abs(spa.azm_rotation) > 360) {
                return 15;
            }
        }

        return 0;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    public static double julianDay(int year, int month, int day, int hour, int minute, double second, double dut1, double tz) {
        double day_decimal, julian_day, a;

        day_decimal = day + (hour - tz + (minute + (second + dut1) / 60.0) / 60.0) / 24.0;

        if (month < 3) {
            month += 12;
            year--;
        }

        julian_day = Double.valueOf(365.25 * (year + 4716.0)).intValue() + Double.valueOf(30.6001 * (month + 1)).shortValue() + day_decimal - 1524.5;

        if (julian_day > 2299160.0) {
            a = Double.valueOf(year / 100.0).shortValue();
            julian_day += (2 - a + Double.valueOf(a / 4).shortValue());
        }

        return julian_day;
    }

    public static double julianCentury(double jd) {
        return (jd - 2451545.0) / 36525.0;
    }

    public static double julianEphemerisDay(double jd, double delta_t) {
        return jd + delta_t / 86400.0;
    }

    public static double julianEphemerisCentury(double jde) {
        return (jde - 2451545.0) / 36525.0;
    }

    public static double julianEphemerisMillennium(double jce) {
        return (jce / 10.0);
    }

    public static double earthPeriodicTermSummation(double[][] terms, int count, double jme) {
        int i;
        double sum = 0;

        for (i = 0; i < count; i++) {
            sum += terms[i][Term.TERM_A.ordinal()] * Math.cos(terms[i][Term.TERM_B.ordinal()] + terms[i][Term.TERM_C.ordinal()] * jme);
        }

        return sum;
    }

    public static double earthValues(double[] term_sum, int count, double jme) {
        int i;
        double sum = 0;

        for (i = 0; i < count; i++) {
            sum += term_sum[i] * Math.pow(jme, i);
        }

        sum /= 1.0e8;

        return sum;
    }

    public static double earthHeliocentricLongitude(double jme) {
        double[] sum = new double[L_COUNT];
        int i;

        for (i = 0; i < L_COUNT; i++) {
            sum[i] = earthPeriodicTermSummation(SUKU2_L[i], l_subcount[i], jme);
        }

        return limitDegrees(Math.toDegrees(earthValues(sum, L_COUNT, jme)));

    }

    public static double earthHeliocentricLatitude(double jme) {
        double[] sum = new double[B_COUNT];
        int i;

        for (i = 0; i < B_COUNT; i++) {
            sum[i] = earthPeriodicTermSummation(SUKU2_B[i], b_subcount[i], jme);
        }

        return Math.toDegrees(earthValues(sum, B_COUNT, jme));

    }

    public static double earthRadiusVector(double jme) {
        double[] sum = new double[R_COUNT];
        int i;

        for (i = 0; i < R_COUNT; i++) {
            sum[i] = earthPeriodicTermSummation(SUKU2_R[i], r_subcount[i], jme);
        }

        return earthValues(sum, R_COUNT, jme);

    }

    public static double geocentricLongitude(double l) {
        double theta = l + 180.0;

        if (theta >= 360.0) {
            theta -= 360.0;
        }

        return theta;
    }

    public static double geocentricLatitude(double b) {
        return -b;
    }

    public static double meanElongationMoonSun(double jce) {
        return thirdOrderPolynomial(1.0 / 189474.0, -0.0019142, 445267.11148, 297.85036, jce);
    }

    public static double meanAnomalySun(double jce) {
        return thirdOrderPolynomial(-1.0 / 300000.0, -0.0001603, 35999.05034, 357.52772, jce);
    }

    public static double meanAnomalyMoon(double jce) {
        return thirdOrderPolynomial(1.0 / 56250.0, 0.0086972, 477198.867398, 134.96298, jce);
    }

    public static double argumentLatitudeMoon(double jce) {
        return thirdOrderPolynomial(1.0 / 327270.0, -0.0036825, 483202.017538, 93.27191, jce);
    }

    public static double ascendingLongitudeMoon(double jce) {
        return thirdOrderPolynomial(1.0 / 450000.0, 0.0020708, -1934.136261, 125.04452, jce);
    }

    public static double xyTermSummation(int i, double[] x) {
        int j;
        double sum = 0;

        for (j = 0; j < TermX.TERM_Y_COUNT; j++) {
            sum += x[j] * SUKU2_Y[i][j];
        }

        return sum;
    }

    public static Map<String, Double> nutationLongitudeAndObliquity(double jce, double[] x) {
        int i;
        double xy_term_sum, sum_psi = 0, sum_epsilon = 0;

        for (i = 0; i < Y_COUNT; i++) {
            xy_term_sum = Math.toRadians(xyTermSummation(i, x));
            sum_psi += (SUKU2_PE[i][TermPE.TERM_PSI_A.ordinal()] + jce * SUKU2_PE[i][TermPE.TERM_PSI_B.ordinal()]) * Math.sin(xy_term_sum);
            sum_epsilon += (SUKU2_PE[i][TermPE.TERM_EPS_C.ordinal()] + jce * SUKU2_PE[i][TermPE.TERM_EPS_D.ordinal()]) * Math.cos(xy_term_sum);
        }

        return Map.of(
                "del_psi", sum_psi / 36000000.0,
                "del_epsilon", sum_psi / sum_epsilon / 36000000.0
        );
    }

    public static double eclipticMeanObliquity(double jme) {
        double u = jme / 10.0;

        return 84381.448 + u * (-4680.93 + u * (-1.55 + u * (1999.25 + u * (-51.38 + u * (-249.67
                + u * (-39.05 + u * (7.12 + u * (27.87 + u * (5.79 + u * 2.45)))))))));
    }

    public static double eclipticTrueObliquity(double delta_epsilon, double epsilon0) {
        return delta_epsilon + epsilon0 / 3600.0;
    }

    public static double aberrationCorrection(double r) {
        return -20.4898 / (3600.0 * r);
    }

    public static double apparentSunLongitude(double theta, double delta_psi, double delta_tau) {
        return theta + delta_psi + delta_tau;
    }

    public static double greenwichMeanSiderealTime(double jd, double jc) {
        return limitDegrees(280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + jc * jc * (0.000387933 - jc / 38710000.0));
    }

    public static double greenwichSiderealTime(double nu0, double delta_psi, double epsilon) {
        return nu0 + delta_psi * Math.cos(Math.toRadians(epsilon));
    }

    public static double geocentricRightAscension(double lamda, double epsilon, double beta) {
        double lamda_rad = Math.toRadians(lamda);
        double epsilon_rad = Math.toRadians(epsilon);

        return limitDegrees(Math.toDegrees(Math.atan2(Math.sin(lamda_rad) * Math.cos(epsilon_rad)
                - Math.tan(Math.toRadians(beta)) * Math.sin(epsilon_rad), Math.cos(lamda_rad))));
    }

    public static double geocentricDeclination(double beta, double epsilon, double lamda) {
        double beta_rad = Math.toRadians(beta);
        double epsilon_rad = Math.toRadians(epsilon);

        return Math.toDegrees(Math.asin(Math.sin(beta_rad) * Math.cos(epsilon_rad)
                + Math.cos(beta_rad) * Math.sin(epsilon_rad) * Math.sin(Math.toRadians(lamda))));
    }

    public static double observerHourAngle(double nu, double longitude, double alpha_deg) {
        return limitDegrees(nu + longitude - alpha_deg);
    }

    public static double sunEquatorialHorizontalParallax(double r) {
        return 8.794 / (3600.0 * r);
    }

    public static Map<String, Double> rightAscensionParallaxAndTopocentricDec(double latitude, double elevation,
                                                                              double xi, double h, double delta) {
        //double delta_alpha[], //di C, cukup *delta_alpha dan *delta_prime
        //double delta_prime[]) {

        double delta_alpha_rad;
        double lat_rad = Math.toRadians(latitude);
        double xi_rad = Math.toRadians(xi);
        double h_rad = Math.toRadians(h);
        double delta_rad = Math.toRadians(delta);
        double u = Math.atan(0.99664719 * Math.tan(lat_rad));
        double y = 0.99664719 * Math.sin(u) + elevation * Math.sin(lat_rad) / 6378140.0;
        double x = Math.cos(u) + elevation * Math.cos(lat_rad) / 6378140.0;

        delta_alpha_rad = Math.atan2(-x * Math.sin(xi_rad) * Math.sin(h_rad),
                Math.cos(delta_rad) - x * Math.sin(xi_rad) * Math.cos(h_rad));

        return Map.of(
                "delta_prime", Math.toDegrees(Math.atan2((Math.sin(delta_rad) - y * Math.sin(xi_rad)) * Math.cos(delta_alpha_rad), Math.cos(delta_rad) - x * Math.sin(xi_rad) * Math.cos(h_rad))),
                "delta_alpha", Math.toDegrees(delta_alpha_rad)
        );
    }

    public static double topocentricRightAscension(double alpha_deg, double delta_alpha) {
        return alpha_deg + delta_alpha;
    }

    public static double topocentricLocalHourAngle(double h, double delta_alpha) {
        return h - delta_alpha;
    }

    public static double topocentricElevationAngle(double latitude, double delta_prime, double h_prime) {
        double lat_rad = Math.toRadians(latitude);
        double delta_prime_rad = Math.toRadians(delta_prime);

        return Math.toDegrees(Math.asin(Math.sin(lat_rad) * Math.sin(delta_prime_rad)
                + Math.cos(lat_rad) * Math.cos(delta_prime_rad) * Math.cos(Math.toRadians(h_prime))));
    }

    public static double atmosphericRefractionCorrection(double pressure, double temperature,
                                                         double atmos_refract, double e0) {
        double del_e = 0;

        if (e0 >= -1 * (SUN_RADIUS + atmos_refract)) {
            del_e = (pressure / 1010.0) * (283.0 / (273.0 + temperature))
                    * 1.02 / (60.0 * Math.tan(Math.toRadians(e0 + 10.3 / (e0 + 5.11))));
        }

        return del_e;
    }

    public static double topocentricElevationAngleCorrected(double e0, double delta_e) {
        return e0 + delta_e;
    }

    public static double topocentricZenithAngle(double e) {
        return 90.0 - e;
    }

    public static double topocentricAzimuthAngleAstro(double h_prime, double latitude, double delta_prime) {
        double h_prime_rad = Math.toRadians(h_prime);
        double lat_rad = Math.toRadians(latitude);

        return limitDegrees(Math.toDegrees(Math.atan2(Math.sin(h_prime_rad),
                Math.cos(h_prime_rad) * Math.sin(lat_rad) - Math.tan(Math.toRadians(delta_prime)) * Math.cos(lat_rad))));
    }

    public static double topocentricAzimuthAngle(double azimuth_astro) {
        return limitDegrees(azimuth_astro + 180.0);
    }

    public static double surfaceIncidenceAngle(double zenith, double azimuth_astro, double azm_rotation,
                                               double slope) {
        double zenith_rad = Math.toRadians(zenith);
        double slope_rad = Math.toRadians(slope);

        return Math.toDegrees(Math.acos(Math.cos(zenith_rad) * Math.cos(slope_rad)
                + Math.sin(slope_rad) * Math.sin(zenith_rad) * Math.cos(Math.toRadians(azimuth_astro - azm_rotation))));
    }

    public static double sunMeanLongitude(double jme) {
        return limitDegrees(280.4664567 + jme * (360007.6982779 + jme * (0.03032028
                + jme * (1 / 49931.0 + jme * (-1 / 15300.0 + jme * (-1 / 2000000.0))))));
    }

    public static double eot(double m, double alpha, double del_psi, double epsilon) {
        return limitMinutes(4.0 * (m - 0.0057183 - alpha + del_psi * Math.cos(Math.toRadians(epsilon))));
    }

    public static double approxSunTransitTime(double alpha_zero, double longitude, double nu) {
        return (alpha_zero - longitude - nu) / 360.0;
    }

    // Kerendahan ufuk / dip of horizon [derajat] untuk pengamat di ketinggian h meter di atas
    // permukaan laut: ufuk terlihat turun, jadi matahari terbit lebih awal dan terbenam
    // lebih lambat.
    //
    // Koefisiennya sengaja disamakan dengan praytimes.org (riseSetAngle di PrayTimes.js),
    // yang memakai 0,833 + 0,0347 * sqrt(h) sebagai sudut terbit/terbenam. Di sini 0,833
    // sudah diwakili SUN_RADIUS + atmos_refract, jadi yang ditambahkan tinggal suku dip-nya.
    // Bandingkan: dip geometri murni = 0,0321 * sqrt(h), dan rumus falak klasik yang sudah
    // memperhitungkan refraksi terestrial = 1,76' * sqrt(h) = 0,0293 * sqrt(h). Selisih
    // ketiganya sekitar setengah menit di ketinggian 800 m.
    // false kalau matahari tidak terbit atau tidak terbenam di tanggal itu (lintang tinggi):
    // calculateEotAndSunRiseTransitSet() mengisi -99999 sebagai penanda, dan nilai itu tidak
    // boleh diteruskan ke LocalTime/penjadwalan
    public static boolean rtsSah(SPAData spa) {
        return spa.sunrise > -99998 && spa.sunset > -99998 && spa.suntransit > -99998;
    }

    public static double dipOfHorizon(double elevation) {
        if (!(elevation > 0)) { // termasuk NaN dan ketinggian di bawah permukaan laut
            return 0.0;
        }
        return 0.0347 * Math.sqrt(elevation);
    }

    public static double sunHourAngleAtRiseSet(double latitude, double delta_zero, double h0_prime) {
        double h0 = -99999;
        double latitude_rad = Math.toRadians(latitude);
        double delta_zero_rad = Math.toRadians(delta_zero);
        double argument = (Math.sin(Math.toRadians(h0_prime)) - Math.sin(latitude_rad) * Math.sin(delta_zero_rad))
                / (Math.cos(latitude_rad) * Math.cos(delta_zero_rad));

        if (Math.abs(argument) <= 1) {
            h0 = limitDegrees180(Math.toDegrees(Math.acos(argument)));
        }

        return h0;
    }

    public static void approxSunRiseAndSet(double[] m_rts, double h0) {
        double h0_dfrac = h0 / 360.0;

        m_rts[Sun.SUN_RISE.ordinal()] = limitZero2one(m_rts[Sun.SUN_TRANSIT.ordinal()] - h0_dfrac);
        m_rts[Sun.SUN_SET.ordinal()] = limitZero2one(m_rts[Sun.SUN_TRANSIT.ordinal()] + h0_dfrac);
        m_rts[Sun.SUN_TRANSIT.ordinal()] = limitZero2one(m_rts[Sun.SUN_TRANSIT.ordinal()]);
    }

    public static double rtsAlphaDeltaPrime(double[] ad, double n) {
        double a = ad[JD.JD_ZERO.ordinal()] - ad[JD.JD_MINUS.ordinal()];
        double b = ad[JD.JD_PLUS.ordinal()] - ad[JD.JD_ZERO.ordinal()];

        if (Math.abs(a) >= 2.0) {
            a = limitZero2one(a);
        }
        if (Math.abs(b) >= 2.0) {
            b = limitZero2one(b);
        }

        return ad[JD.JD_ZERO.ordinal()] + n * (a + b + (b - a) * n) / 2.0;
    }

    public static double rtsRunAltitude(double latitude, double delta_prime, double h_prime) {
        double latitude_rad = Math.toRadians(latitude);
        double delta_prime_rad = Math.toRadians(delta_prime);

        return Math.toDegrees(Math.asin(Math.sin(latitude_rad) * Math.sin(delta_prime_rad)
                + Math.cos(latitude_rad) * Math.cos(delta_prime_rad) * Math.cos(Math.toRadians(h_prime))));
    }

    public static double sunRiseAndSet(double[] m_rts, double[] h_rts, double[] delta_prime, double latitude,
                                       double[] h_prime, double h0_prime, int sun) {
        return m_rts[sun] + (h_rts[sun] - h0_prime)
                / (360.0 * Math.cos(Math.toRadians(delta_prime[sun])) * Math.cos(Math.toRadians(latitude)) * Math.sin(Math.toRadians(h_prime[sun])));
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////
    public static void calculateGeocentricSunRightAscensionAndDeclination(SPAData spa) {
        double[] x = new double[TermX.TERM_X_COUNT];

        spa.jc = julianCentury(spa.jd);

        spa.jde = julianEphemerisDay(spa.jd, spa.delta_t);
        spa.jce = julianEphemerisCentury(spa.jde);
        spa.jme = julianEphemerisMillennium(spa.jce);

        spa.l = earthHeliocentricLongitude(spa.jme);
        spa.b = earthHeliocentricLatitude(spa.jme);
        spa.r = earthRadiusVector(spa.jme);

        spa.theta = geocentricLongitude(spa.l);
        spa.beta = geocentricLatitude(spa.b);

        x[TermX.TERM_X0.ordinal()] = spa.x0 = meanElongationMoonSun(spa.jce);
        x[TermX.TERM_X1.ordinal()] = spa.x1 = meanAnomalySun(spa.jce);
        x[TermX.TERM_X2.ordinal()] = spa.x2 = meanAnomalyMoon(spa.jce);
        x[TermX.TERM_X3.ordinal()] = spa.x3 = argumentLatitudeMoon(spa.jce);
        x[TermX.TERM_X4.ordinal()] = spa.x4 = ascendingLongitudeMoon(spa.jce);

        Map<String, Double> mapDel = nutationLongitudeAndObliquity(spa.jce, x);

        spa.del_psi = mapDel.get("del_psi");
        spa.del_epsilon = mapDel.get("del_epsilon");
        spa.epsilon0 = eclipticMeanObliquity(spa.jme);
        spa.epsilon = eclipticTrueObliquity(spa.del_epsilon, spa.epsilon0);

        spa.del_tau = aberrationCorrection(spa.r);

        spa.lamda = apparentSunLongitude(spa.theta, spa.del_psi, spa.del_tau);

        spa.nu0 = greenwichMeanSiderealTime(spa.jd, spa.jc);
        spa.nu = greenwichSiderealTime(spa.nu0, spa.del_psi, spa.epsilon);

        spa.alpha = geocentricRightAscension(spa.lamda, spa.epsilon, spa.beta);
        spa.delta = geocentricDeclination(spa.beta, spa.epsilon, spa.lamda);
    }

    /// /////////////////////////////////////////////////////////////////////
    public static void calculateEotAndSunRiseTransitSet(SPAData spa) {
        SPAData sun_rts;
        double nu, m, h0, n;
        double[] alpha = new double[JD.JD_COUNT], delta = new double[JD.JD_COUNT];
        double[] m_rts = new double[Sun.SUN_COUNT], nu_rts = new double[Sun.SUN_COUNT], h_rts = new double[Sun.SUN_COUNT];
        double[] alpha_prime = new double[Sun.SUN_COUNT], delta_prime = new double[Sun.SUN_COUNT], h_prime = new double[Sun.SUN_COUNT];
        double h0_prime = -1 * (SUN_RADIUS + spa.atmos_refract + dipOfHorizon(spa.elevation));
        int i;

        try {
            //di c cukup, sun_rts = *spa
            sun_rts = (SPAData) spa.clone();

            m = sunMeanLongitude(spa.jme);
            spa.eot = eot(m, spa.alpha, spa.del_psi, spa.epsilon);

            sun_rts.hour = sun_rts.minute = 0;
            sun_rts.second = 0;
            sun_rts.delta_ut1 = sun_rts.timezone = 0.0;

            sun_rts.jd = julianDay(sun_rts.year, sun_rts.month, sun_rts.day, sun_rts.hour,
                    sun_rts.minute, sun_rts.second, sun_rts.delta_ut1, sun_rts.timezone);

            calculateGeocentricSunRightAscensionAndDeclination(sun_rts);
            nu = sun_rts.nu;

            sun_rts.delta_t = 0;
            sun_rts.jd--;
            for (i = 0; i < JD.JD_COUNT; i++) {
                calculateGeocentricSunRightAscensionAndDeclination(sun_rts);
                alpha[i] = sun_rts.alpha;
                delta[i] = sun_rts.delta;
                sun_rts.jd++;
            }

            m_rts[Sun.SUN_TRANSIT.ordinal()] = approxSunTransitTime(alpha[JD.JD_ZERO.ordinal()], spa.longitude, nu);
            h0 = sunHourAngleAtRiseSet(spa.latitude, delta[JD.JD_ZERO.ordinal()], h0_prime);

            if (h0 >= 0) {
                approxSunRiseAndSet(m_rts, h0);
                for (i = 0; i < Sun.SUN_COUNT; i++) {
                    nu_rts[i] = nu + 360.985647 * m_rts[i];
                    n = m_rts[i] + spa.delta_t / 86400.0;
                    alpha_prime[i] = rtsAlphaDeltaPrime(alpha, n);
                    delta_prime[i] = rtsAlphaDeltaPrime(delta, n);
                    h_prime[i] = limitDegrees180pm(nu_rts[i] + spa.longitude - alpha_prime[i]);
                    h_rts[i] = rtsRunAltitude(spa.latitude, delta_prime[i], h_prime[i]);
                }
                spa.srha = h_prime[Sun.SUN_RISE.ordinal()];
                spa.ssha = h_prime[Sun.SUN_SET.ordinal()];
                spa.sta = h_rts[Sun.SUN_TRANSIT.ordinal()];
                spa.suntransit = dayfracToLocalHr(m_rts[Sun.SUN_TRANSIT.ordinal()] - h_prime[Sun.SUN_TRANSIT.ordinal()] / 360.0,
                        spa.timezone);
                spa.sunrise = dayfracToLocalHr(sunRiseAndSet(m_rts, h_rts, delta_prime,
                        spa.latitude, h_prime, h0_prime, Sun.SUN_RISE.ordinal()), spa.timezone);
                spa.sunset = dayfracToLocalHr(sunRiseAndSet(m_rts, h_rts, delta_prime,
                        spa.latitude, h_prime, h0_prime, Sun.SUN_SET.ordinal()), spa.timezone);
            } else {
                spa.srha = spa.ssha = spa.sta = spa.suntransit = spa.sunrise = spa.sunset = -99999;
            }

        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(SolarPosition.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /// ////////////////////////////////////////////////////////////////////////////////////////
    public static void spaCalculate(SPAData spa) {
        int result;

        result = validateInputs(spa);
        if (result == 0) {
            spa.jd = julianDay(spa.year, spa.month, spa.day, spa.hour,
                    spa.minute, spa.second, spa.delta_ut1, spa.timezone);

            calculateGeocentricSunRightAscensionAndDeclination(spa);

            spa.h = observerHourAngle(spa.nu, spa.longitude, spa.alpha);
            spa.xi = sunEquatorialHorizontalParallax(spa.r);

            Map<String, Double> deltaMap = rightAscensionParallaxAndTopocentricDec(spa.latitude, spa.elevation, spa.xi,
                    spa.h, spa.delta);
            spa.del_alpha = deltaMap.get("delta_alpha");
            spa.delta_prime = deltaMap.get("delta_prime");

            spa.alpha_prime = topocentricRightAscension(spa.alpha, spa.del_alpha);
            spa.h_prime = topocentricLocalHourAngle(spa.h, spa.del_alpha);

            spa.e0 = topocentricElevationAngle(spa.latitude, spa.delta_prime, spa.h_prime);
            spa.del_e = atmosphericRefractionCorrection(spa.pressure, spa.temperature,
                    spa.atmos_refract, spa.e0);
            spa.e = topocentricElevationAngleCorrected(spa.e0, spa.del_e);

            spa.zenith = topocentricZenithAngle(spa.e);
            spa.azimuth_astro = topocentricAzimuthAngleAstro(spa.h_prime, spa.latitude,
                    spa.delta_prime);
            spa.azimuth = topocentricAzimuthAngle(spa.azimuth_astro);

            if ((spa.function == SPA.SPA_ZA_INC) || (spa.function == SPA.SPA_ALL)) {
                spa.incidence = surfaceIncidenceAngle(spa.zenith, spa.azimuth_astro,
                        spa.azm_rotation, spa.slope);
            }

            if ((spa.function == SPA.SPA_ZA_RTS) || (spa.function == SPA.SPA_ALL)) {
                calculateEotAndSunRiseTransitSet(spa);
            }
        }
    }

    //enumeration for function codes to select desired final outputs from SPA
    public enum SPA {
        SPA_ZA, //calculate zenith and azimuth
        SPA_ZA_INC, //calculate zenith, azimuth, and incidence
        SPA_ZA_RTS, //calculate zenith, azimuth, and sun rise/transit/set values
        SPA_ALL  //calculate all SPA output values
    }

    public enum Term {
        TERM_A, TERM_B, TERM_C;

        public static final int TERM_COUNT = Term.values().length;
    }

    public enum TermX {
        TERM_X0, TERM_X1, TERM_X2, TERM_X3, TERM_X4;

        public static final int TERM_X_COUNT = TermX.values().length;
        public static final int TERM_Y_COUNT = TermX.values().length;
    }

    public enum TermPE {
        TERM_PSI_A, TERM_PSI_B, TERM_EPS_C, TERM_EPS_D;

        public static final int TERM_PE_COUNT = TermPE.values().length;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////
    // Calculate required SPA parameters to get the right ascension (alpha) and declination (delta)
    // Note: JD must be already calculated and in structure

    public enum JD {
        JD_MINUS, JD_ZERO, JD_PLUS;

        public static final int JD_COUNT = JD.values().length;
    }

    /// /////////////////////////////////////////////////////////////////////
    // Calculate Equation of Time (EOT) and Sun Rise, Transit, & Set (RTS)

    public enum Sun {
        SUN_TRANSIT, SUN_RISE, SUN_SET;

        public static final int SUN_COUNT = Sun.values().length;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////
// Calculate all SPA parameters and put into structure
// Note: All inputs values (listed in header file) must already be in structure

    public static class SPAData implements Cloneable {

        //----------------------INPUT VALUES------------------------
        public int year;            // 4-digit year,      valid range: -2000 to 6000, error code: 1
        public int month;           // 2-digit month,         valid range: 1 to  12,  error code: 2
        public int day;             // 2-digit day,           valid range: 1 to  31,  error code: 3
        public int hour;            // Observer local hour,   valid range: 0 to  24,  error code: 4
        public int minute;          // Observer local minute, valid range: 0 to  59,  error code: 5
        public double second;       // Observer local second, valid range: 0 to <60,  error code: 6

        public double delta_ut1;    // Fractional second difference between UTC and UT which is used
        // to adjust UTC for earth's irregular rotation rate and is derived
        // from observation only and is reported in this bulletin:
        // http://maia.usno.navy.mil/ser7/ser7.dat,
        // where delta_ut1 = DUT1
        // valid range: -1 to 1 second (exclusive), error code 17

        public double delta_t;      // Difference between earth rotation time and terrestrial time
        // It is derived from observation only and is reported in this
        // bulletin: http://maia.usno.navy.mil/ser7/ser7.dat,
        // where delta_t = 32.184 + (TAI-UTC) - DUT1
        // valid range: -8000 to 8000 seconds, error code: 7

        public double timezone;     // Observer time zone (negative west of Greenwich)
        // valid range: -18   to   18 hours,   error code: 8

        public double longitude;    // Observer longitude (negative west of Greenwich)
        // valid range: -180  to  180 degrees, error code: 9

        public double latitude;     // Observer latitude (negative south of equator)
        // valid range: -90   to   90 degrees, error code: 10

        public double elevation;    // Observer elevation [meters]
        // valid range: -6500000 or higher meters,    error code: 11

        public double pressure;     // Annual average local pressure [millibars]
        // valid range:    0 to 5000 millibars,       error code: 12

        public double temperature;  // Annual average local temperature [degrees Celsius]
        // valid range: -273 to 6000 degrees Celsius, error code; 13

        public double slope;        // Surface slope (measured from the horizontal plane)
        // valid range: -360 to 360 degrees, error code: 14

        public double azm_rotation; // Surface azimuth rotation (measured from south to projection of
        //     surface normal on horizontal plane, negative east)
        // valid range: -360 to 360 degrees, error code: 15

        public double atmos_refract;// Atmospheric refraction at sunrise and sunset (0.5667 deg is typical)
        // valid range: -5   to   5 degrees, error code: 16

        public SPA function;        // Switch to choose functions for desired output (from enumeration)

        //-----------------Intermediate OUTPUT VALUES--------------------
        public double jd;          //Julian day
        public double jc;          //Julian century

        public double jde;         //Julian ephemeris day
        public double jce;         //Julian ephemeris century
        public double jme;         //Julian ephemeris millennium

        public double l;           //earth heliocentric longitude [degrees]
        public double b;           //earth heliocentric latitude [degrees]
        public double r;           //earth radius vector [Astronomical Units, AU]

        public double theta;       //geocentric longitude [degrees]
        public double beta;        //geocentric latitude [degrees]

        public double x0;          //mean elongation (moon-sun) [degrees]
        public double x1;          //mean anomaly (sun) [degrees]
        public double x2;          //mean anomaly (moon) [degrees]
        public double x3;          //argument latitude (moon) [degrees]
        public double x4;          //ascending longitude (moon) [degrees]

        public double del_psi;     //nutation longitude [degrees]
        public double del_epsilon; //nutation obliquity [degrees]
        public double epsilon0;    //ecliptic mean obliquity [arc seconds]
        public double epsilon;     //ecliptic true obliquity  [degrees]

        public double del_tau;     //aberration correction [degrees]
        public double lamda;       //apparent sun longitude [degrees]
        public double nu0;         //Greenwich mean sidereal time [degrees]
        public double nu;          //Greenwich sidereal time [degrees]

        public double alpha;       //geocentric sun right ascension [degrees]
        public double delta;       //geocentric sun declination [degrees]

        public double h;           //observer hour angle [degrees]
        public double xi;          //sun equatorial horizontal parallax [degrees]
        public double del_alpha;   //sun right ascension parallax [degrees]
        public double delta_prime; //topocentric sun declination [degrees]
        public double alpha_prime; //topocentric sun right ascension [degrees]
        public double h_prime;     //topocentric local hour angle [degrees]

        public double e0;          //topocentric elevation angle (uncorrected) [degrees]
        public double del_e;       //atmospheric refraction correction [degrees]
        public double e;           //topocentric elevation angle (corrected) [degrees]

        public double eot;         //equation of time [minutes]
        public double srha;        //sunrise hour angle [degrees]
        public double ssha;        //sunset hour angle [degrees]
        public double sta;         //sun transit altitude [degrees]

        //---------------------Final OUTPUT VALUES------------------------
        public double zenith;       //topocentric zenith angle [degrees]
        public double azimuth_astro;//topocentric azimuth angle (westward from south) [for astronomers]
        public double azimuth;      //topocentric azimuth angle (eastward from north) [for navigators and solar radiation]
        public double incidence;    //surface incidence angle [degrees]

        public double suntransit;   //local sun transit time (or solar noon) [fractional hour]
        public double sunrise;      //local sunrise time (+/- 30 seconds) [fractional hour]
        public double sunset;       //local sunset time (+/- 30 seconds) [fractional hour]

        @NonNull
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}