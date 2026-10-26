package com.zelix.klassmaster.constant;

public abstract class LicenseCheckConstants3 extends LicenseCheckConstants4 {
	public static final String javaVMVersion = System.getProperty("java.vm.version", "unknown");

	public String getExpectedScrambledName(int index) {
		switch (index) {
			case 0:
				return "]YBQ\u001b]X\\R\u0019|&[T&7]W@VlY\"\\%\u001d\tP(&QMZPOU\u001a*Q+";
			case 1:
				return "Y$4VnXT\\#n\t R\\%@($GV\u001a[TZ&\u0016xQ]\\!B[P5Q\u001aXYY";
			case 2:
				return "^PG#\u0018Z#\\_\u0018~P^\\S1,T4X\u001f.VW_\u001b}$.S%M_%E k\\'Z";
			case 3:
				return "\\#CQi(T[!l|'/UT@(V4R\u001aY\"(Tm\rZ/#RM_YB\"\u0019. X";
			default:
				return "( FVl_R*Q\u001dv[_TWM_U7U\u001aYWW^iz#,\\ @YUGPkZV*";
		}
	}
}
