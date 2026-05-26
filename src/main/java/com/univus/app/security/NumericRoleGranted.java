package com.univus.app.security;


	public class NumericRoleGranted {
		public final static int INACTIVE = 0; //비회원
		public final static int USER = 0; //회원
		public final static int PARTNER = 50; //파트너
		public final static int ADMIN = 99; //관리자
		
		public static int getUserLevel(String authority) {
			try {
				switch(authority) {
				case "USER" : return USER;
				case "PARTNER" : return PARTNER;
				case "ADMIN" : return ADMIN;
				}
			} catch (Exception e) {
			}
			return 0;
		}
}
