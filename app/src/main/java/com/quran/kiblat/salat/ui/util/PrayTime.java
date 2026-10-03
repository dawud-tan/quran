package com.quran.kiblat.salat.ui.util;

import com.quran.kiblat.salat.ui.util.SolarPosition.SPAData;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PrayTime {

    public SPAData spa;
    // ---------------------- Global Variables --------------------
    public double lat; // latitude
    public double JDate; // Julian date
    // ------------------------------------------------------------

    // Time Names
    public ArrayList<String> timeNames;

    // ------------------- Calc Method Parameters --------------------
    public HashMap<String, double[]> methodParams;

    /*
     * this.methodParams[methodNum] = new Array(fa, ms, mv, is, iv);
     *
     * fa : fajr angle ms : maghrib selector (0 = angle; 1 = minutes after
     * sunset) mv : maghrib parameter value (in angle or minutes) is : isha
     * selector (0 = angle; 1 = minutes after maghrib) iv : isha parameter value
     * (in angle or minutes)
     */
    public int[] offsets;

    // Faktor panjang bayangan penanda masuknya Ashar:
    // 1 = Syafi'i, Maliki, Hanbali (jumhur); 2 = Hanafi
    public int asrFactor = 1;

    public PrayTime() {
        // Initialize vars

        // Time Names
        timeNames = new ArrayList<>();
        timeNames.add("Subuh");
        timeNames.add("Sunrise");
        timeNames.add("Dhuhr");
        timeNames.add("Asr");
        timeNames.add("Sunset");
        timeNames.add("Maghrib");
        timeNames.add("Isha");

        // ------------------- Calc Method Parameters --------------------
        // Tuning offsets {fajr, sunrise, dhuhr, asr, sunset, maghrib, isha}
        offsets = new int[7];
        offsets[0] = 0;
        offsets[1] = 0;
        offsets[2] = 0;
        offsets[3] = 0;
        offsets[4] = 0;
        offsets[5] = 0;
        offsets[6] = 0;

        /*
         *
         * fa : fajr angle ms : maghrib selector (0 = angle; 1 = minutes after
         * sunset) mv : maghrib parameter value (in angle or minutes) is : isha
         * selector (0 = angle; 1 = minutes after maghrib) iv : isha parameter
         * value (in angle or minutes)
         */
        methodParams = new HashMap<>();

        methodParams.put("falakiyah", new double[]{20, 0, 0, 0, 18});//indonesia, subuh -20.0, isya -18.0

    }

    // ---------------------- Trigonometric Functions -----------------------
    // range reduce hours to 0..23
    public double fixhour(double a) {
        a = a - 24.0 * Math.floor(a / 24.0);
        a = a < 0 ? (a + 24) : a;
        return a;
    }

    // degree sin
    public double dsin(double d) {
        return (Math.sin(Math.toRadians(d)));
    }

    // degree cos
    public double dcos(double d) {
        return (Math.cos(Math.toRadians(d)));
    }

    // degree tan
    public double dtan(double d) {
        return (Math.tan(Math.toRadians(d)));
    }

    // degree arccos
    public double darccos(double x) {
        double val = Math.acos(x);
        return Math.toDegrees(val);
    }

    // degree arccot
    public double darccot(double x) {
        double val = Math.atan2(1.0, x);
        return Math.toDegrees(val);
    }

    // ---------------------- Calculation Functions -----------------------
    // References:
    // I. Reda and A. Andreas, “Solar position algorithm for solar radiation applications,” Solar Energy, vol. 76, no. 5, pp. 577–589, 2004
    // compute declination angle of sun and equation of time
    public double[] sunPosition(double jd) {
        double jde = SolarPosition.julianEphemerisDay(jd, SolarPosition.DELTA_T);
        double jce = SolarPosition.julianEphemerisCentury(jde);
        double jme = SolarPosition.julianEphemerisMillennium(jce);
        double l = SolarPosition.earthHeliocentricLongitude(jme);
        double b = SolarPosition.earthHeliocentricLatitude(jme);
        double r = SolarPosition.earthRadiusVector(jme);
        double beta = SolarPosition.geocentricLatitude(b);
        double[] x = new double[SolarPosition.TermX.TERM_X_COUNT];
        Map<String, Double> mapDel = SolarPosition.nutationLongitudeAndObliquity(jce, x);
        double del_psi = mapDel.get("del_psi");
        double del_epsilon = mapDel.get("del_epsilon");
        double epsilon0 = SolarPosition.eclipticMeanObliquity(jme);
        double epsilon = SolarPosition.eclipticTrueObliquity(del_epsilon, epsilon0);
        double theta = SolarPosition.geocentricLongitude(l);
        double del_tau = SolarPosition.aberrationCorrection(r);
        double lamda = SolarPosition.apparentSunLongitude(theta, del_psi, del_tau);
        double alpha = SolarPosition.geocentricRightAscension(lamda, epsilon, beta);
        double m = SolarPosition.sunMeanLongitude(jme);

        double[] sPosition = new double[2];
        sPosition[0] = SolarPosition.geocentricDeclination(beta, epsilon, lamda);
        sPosition[1] = SolarPosition.eot(m, alpha, del_psi, epsilon) / 60.0; //dibagi 60 menit, satuan biar jadi jam

        return sPosition;
    }

    // compute equation of time
    public double equationOfTime(double jd) {
        return sunPosition(jd)[1];
    }

    // compute declination angle of sun
    public double sunDeclination(double jd) {
        return sunPosition(jd)[0];
    }

    // compute mid-day (Dhuhr, Zawal) time
    public double computeMidDay(double t) {
        double T = equationOfTime(this.getJDate() + t);
        return fixhour(12 - T);
    }

    // compute time for a given angle G
    public double computeTime(double G, double t) {
        return computeTime(G, sunPosition(this.getJDate() + t));
    }

    // deklinasi dan equation of time dipakai berdua, jadi cukup dihitung sekali
    private double computeTime(double G, double[] sPosition) {
        double D = sPosition[0];
        double Z = fixhour(12 - sPosition[1]);
        double Beg = -dsin(G) - dsin(D) * dsin(this.getLat());
        double Mid = dcos(D) * dcos(this.getLat());
        double V = darccos(Beg / Mid) / 15.0;

        return Z + (G > 90 ? -V : V);
    }

    // compute the time of Asr
    // Shafii: step=1
    public double computeAsr(double step, double t) {
        double[] sPosition = sunPosition(this.getJDate() + t);
        double G = -darccot(step + dtan(Math.abs(this.getLat() - sPosition[0])));
        return computeTime(G, sPosition);
    }

    // ---------------------- Misc Functions -----------------------
    // compute the difference between two times
    public double timeDiff(double time1, double time2) {
        return fixhour(time2 - time1);
    }

    // -------------------- Interface Functions --------------------
    // return prayer times for a given date
    public ArrayList<ZonedDateTime> getDatePrayerTimes(SPAData spa) {
        this.spa = spa;
        this.setLat(spa.latitude);
        // Julian day 0h UT tanggal setempat dikurangi bujur: computeTime() memakai t sebagai
        // pecahan hari dihitung dari tengah malam waktu setempat, jadi tambatannya harus
        // tengah malam juga. Kalau dipakai jam saat perhitungan dijalankan, deklinasi matahari
        // diambil pada saat yang salah dan jadwalnya bergeser (sampai ~50 detik) tergantung
        // kapan penjadwalan ulang kebetulan terjadi.
        this.setJDate(SolarPosition.julianDay(spa.year, spa.month, spa.day, 0, 0, 0, 0, 0)
                - spa.longitude / (15.0 * 24.0));
        return computeDayTimes();
    }

    // convert double hours to 24h format
    public ZonedDateTime floatToTime24(double time) {
        double hour = fixhour(time + 0.008333333333333d); // tambahkan 0.00833 jam = 30 detik untuk bulatkan
        double minutes = (hour - (int) hour) * 60.0;

        // detik dan nanodetik sengaja dinolkan: 30 detik di atas adalah pembulatan ke menit
        // terdekat, kalau sisa detiknya ikut dibawa semua waktu jadi 30 detik kesiangan
        return ZonedDateTime.of(spa.year, spa.month, spa.day,
                Double.valueOf(hour).intValue(),
                Double.valueOf(minutes).intValue(),
                0,
                0,
                ZoneId.systemDefault());
    }

    // ---------------------- Compute Prayer Times -----------------------
    // compute prayer times at given julian date
    public ArrayList<ZonedDateTime> computeDayTimes() {
        // jd dan posisi matahari geosentris perlu diisi dulu, kalau tidak
        // calculateEotAndSunRiseTransitSet() menghitung spa.eot dari field yang masih kosong
        spa.jd = SolarPosition.julianDay(spa.year, spa.month, spa.day, spa.hour,
                spa.minute, spa.second, spa.delta_ut1, spa.timezone);
        SolarPosition.calculateGeocentricSunRightAscensionAndDeclination(spa);
        SolarPosition.calculateEotAndSunRiseTransitSet(spa);

        double[] falakiyah = Objects.requireNonNull(methodParams.get("falakiyah"));

        // Maghrib memakai selektor sudut 0 derajat, artinya persis saat terbenam.
        // Terbenam dari SPA (spa.sunset) sudah memperhitungkan refraksi, semidiameter,
        // dan kerendahan ufuk, jadi diambil langsung, bukan lewat computeTime(0, ...)
        // yang menghasilkan saat pusat matahari di ufuk hakiki.
        boolean maghribSaatTerbenam = falakiyah[1] == 0 && falakiyah[2] == 0;

        double[] times = {5, 6, 12, 13, 18, 18, 18}; // default times

        // convert hours to day portions
        for (int i = 0; i < 7; i++) {
            times[i] /= 24;
        }

        //Subuh
        times[0] = this.computeTime(180 - falakiyah[0], times[0]);

        //Ashar
        times[3] = this.computeAsr(asrFactor, times[3]);

        //Maghrib
        if (!maghribSaatTerbenam) {
            times[5] = this.computeTime(falakiyah[2], times[5]);
        }

        //Isya
        times[6] = this.computeTime(falakiyah[4], times[6]);

        // adjust times in a prayer time array
        for (int i = 0; i < times.length; i++) {
            times[i] += spa.timezone - spa.longitude / 15;
        }

        //terbit
        times[1] = spa.sunrise;

        //Dhuhur
        times[2] = spa.suntransit;

        //Sunset
        times[4] = spa.sunset;

        if (maghribSaatTerbenam) {
            times[5] = times[4];
        }

        if (falakiyah[1] == 1) // Maghrib
        {
            times[5] = times[4] + falakiyah[2] / 60;
        }
        if (falakiyah[3] == 1) // Isha
        {
            times[6] = times[5] + falakiyah[4] / 60;
        }

        // adjust Subuh, Isha and Maghrib for locations in higher latitudes
        double nightTime = timeDiff(times[4], times[1]); // sunset to sunrise

        // Adjust Subuh
        double SubuhDiff = nightPortion(falakiyah[0]) * nightTime;

        if (Double.isNaN(times[0]) || timeDiff(times[0], times[1]) > SubuhDiff) {
            times[0] = times[1] - SubuhDiff;
        }

        // Adjust Isha
        double IshaAngle = (falakiyah[3] == 0) ? falakiyah[4] : 18;
        double IshaDiff = this.nightPortion(IshaAngle) * nightTime;
        if (Double.isNaN(times[6]) || this.timeDiff(times[4], times[6]) > IshaDiff) {
            times[6] = times[4] + IshaDiff;
        }

        // Adjust Maghrib
        double MaghribAngle = (falakiyah[1] == 0) ? falakiyah[2] : 4;
        double MaghribDiff = nightPortion(MaghribAngle) * nightTime;
        if (Double.isNaN(times[5]) || this.timeDiff(times[4], times[5]) > MaghribDiff) {
            times[5] = times[4] + MaghribDiff;
        }

        for (int i = 0; i < times.length; i++) {
            times[i] = times[i] + this.offsets[i] / 60.0;
        }

        ArrayList<ZonedDateTime> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            result.add(floatToTime24(times[i]));
        }
        return result;
    }

    // the night portion used for adjusting times in higher latitudes
    public double nightPortion(double angle) {
        return angle / 60.0;
    }

    public void setAsrFactor(int asrFactor) {
        this.asrFactor = asrFactor;
    }

    // Tune timings for adjustments
    // Set time offsets
    public void tune(int[] offsetTimes) {
        // should be 7 in order
        // of Subuh, Sunrise,
        // Dhuhr, Asr, Sunset,
        // Maghrib, Isha
        this.offsets = offsetTimes.clone();
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getJDate() {
        return JDate;
    }

    public void setJDate(double jDate) {
        JDate = jDate;
    }

}