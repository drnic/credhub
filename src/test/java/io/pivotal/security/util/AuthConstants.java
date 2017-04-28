package io.pivotal.security.util;

@SuppressWarnings("checkstyle:linelength")
public class AuthConstants {
  // JWT token signed by private key for public key in `application-unit-test.yml`
  // Valid for about 50 years!!!
  // Check and change at jwt.io

  // Actor ID: uaa-user:df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  // Grant type: password
  // Client ID: credhub_cli
  // User ID: df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  public static final String UAA_OAUTH2_PASSWORD_GRANT_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiI5YTk3YWVlNjVhYWY0Yzc0ODVhMTZmZWU1MWY5NTVmMSIsInN1YiI6ImRmMGMxYTI2LTI4NzUtNGJmNS1iYWY5LTcxNmM2YmI1ZWE2ZCIsInNjb3BlIjpbImNyZWRodWIud3JpdGUiLCJjcmVkaHViLnJlYWQiXSwiY2xpZW50X2lkIjoiY3JlZGh1Yl9jbGkiLCJjaWQiOiJjcmVkaHViX2NsaSIsImF6cCI6ImNyZWRodWJfY2xpIiwiZ3JhbnRfdHlwZSI6InBhc3N3b3JkIiwidXNlcl9pZCI6ImRmMGMxYTI2LTI4NzUtNGJmNS1iYWY5LTcxNmM2YmI1ZWE2ZCIsIm9yaWdpbiI6InVhYSIsInVzZXJfbmFtZSI6ImNyZWRodWJfY2xpIiwiZW1haWwiOiJjcmVkaHViX2NsaSIsImF1dGhfdGltZSI6MTQ5MDkwNjA5MCwicmV2X3NpZyI6ImU0NDNkNzFlIiwiaWF0IjoxNDkwOTAzMzUzLCJleHAiOjMwNjc3MDYwOTAsImlzcyI6Imh0dHBzOi8vMTAuMjQ0LjAuMjo4NDQzL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNyZWRodWJfY2xpIiwiY3JlZGh1YiJdfQ.CAcgmH3Phz19du8TljLSpPFsCcGHU5dMF1QxCY_TQkoPHPYPDNmequ7i1TbjLGvmU3VV1JgFumNp9BZyeY-6erjCGLi8yIlAUT0Eu3XLqGAQ7vK61EostwKZ-MOm6xe1Wma0xzcXlm-leJQasVF-Ta5mO4hOLSsSB1kJ4pTCCSbhJwCEE5ZwK_9RRXRWEmDXCXJHcN19WP1fW7lhx_fa8XF4bVffehpBaYvp6az64LbqxIRBWVfgxB0Qr4nkIGC-bsi5F01w9K4tFo2n5oSwc9Kj9NR2WjceuOB9E9DwzKbySF2IO9KUmdWLDKVSXVWgsD7HGkAFfOHrFQlNruYylg";

  // Actor ID: uaa-client:credhub_test
  // Grant type: client_credentials
  // Client ID: credhub_test
  public static final String UAA_OAUTH2_CLIENT_CREDENTIALS_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiI0NWRjYTFiM2UzMGY0NDhjYjE5Y2U4YTVkYzRhMDdmYyIsInN1YiI6ImNyZWRodWJfdGVzdCIsImF1dGhvcml0aWVzIjpbImNyZWRodWIud3JpdGUiLCJjcmVkaHViLnJlYWQiXSwic2NvcGUiOlsiY3JlZGh1Yi53cml0ZSIsImNyZWRodWIucmVhZCJdLCJjbGllbnRfaWQiOiJjcmVkaHViX3Rlc3QiLCJjaWQiOiJjcmVkaHViX3Rlc3QiLCJhenAiOiJjcmVkaHViX3Rlc3QiLCJncmFudF90eXBlIjoiY2xpZW50X2NyZWRlbnRpYWxzIiwicmV2X3NpZyI6ImRlNTQxYmEwIiwiaWF0IjoxNDkxMjQ0NzgwLCJleHAiOjMwNjgwNDQ3ODAsImlzcyI6Imh0dHBzOi8vMTAuMjQ0LjAuMjo4NDQzL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNyZWRodWJfdGVzdCIsImNyZWRodWIiXX0.nTjfC08MOaKiWsOWWw-Ok0UudaAVd0DwMQmPEyCOENB1tOt2Q9dLb3k3VvSZN00n9rkgJEk5wkPy67VCec9J7efjPlh0mgrME_K8as1Iu0FhoOyIQdw22IyAVw4zVWli8CLaoCghE-M1mAYy2qlBIuSXZa_9RjoeW1qcojcObn8hJawV_RDGJb502Fxxizzsp73ybUUsZ8mf1UR6zDGR1KL9ar3z_Qr_RNku9Qwi1txfkQ4tQJA34MMCRei3LzDeiIORqi2lajlavGeY_TERMawgLuBujMN0Xf9ekqji6LA6oviQkb0BgRuluspgx1oeMef5ePr1FO2AZg2y4l5vKw";

  // Actor ID: uaa-user:df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  // Grant type: password
  // Client ID: credhub_cli
  // User ID: df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  public static final String INVALID_SCOPE_KEY_JWT = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiJlOWU1NzM5Y2QzODc0NDgzOGFjZjY4M2I3YWI0N2IwNCIsInN1YiI6ImRmMGMxYTI2LTI4NzUtNGJmNS1iYWY5LTcxNmM2YmI1ZWE2ZCIsInNjb3BlIjpbImNyZWRodWIuYmFkX3Njb3BlIl0sImNsaWVudF9pZCI6ImNyZWRodWJfY2xpIiwiY2lkIjoiY3JlZGh1Yl9jbGkiLCJhenAiOiJjcmVkaHViX2NsaSIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiJkZjBjMWEyNi0yODc1LTRiZjUtYmFmOS03MTZjNmJiNWVhNmQiLCJvcmlnaW4iOiJ1YWEiLCJ1c2VyX25hbWUiOiJjcmVkaHViX2NsaSIsImVtYWlsIjoiY3JlZGh1Yl9jbGkiLCJhdXRoX3RpbWUiOjE0OTA5MDMzNTMsInJldl9zaWciOiJlNDQzZDcxZSIsImlhdCI6MTQ5MDkwMzM1MywiZXhwIjozNDkwOTAzMzU0LCJpc3MiOiJodHRwczovLzEwLjI0NC4wLjI6ODQ0My9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjcmVkaHViX2NsaSIsImNyZWRodWIiXX0.DvI-nKyuOAAdldi92Zo6PXKQNJwpbg9NtAmXGUiSupnA8rpfx2MIDpYeS7aAR0kvFkpnryLHBZn5zFJi_Vckaff0q9Vjyph8Mgr3OAbMe2SD4oD_Wsejq58gpWxHvezykW_cXdtcLGkYYjz7Pfm4e0o5S_4YFo69ZnwyT6ZYnxCEEYsLJalQfgq6RnrQLIqnoGH6gsxHkBF5GYOt9hhPyUSH-WKUd337zPrflZ5s5-BVgWv5EqBRJDXLbcVwtY89-dtHu8gU0ww7Opbq7JfDm5CzVPXyqfDNWyXl9HGL0C5xOF8zMgmfk406BO5UNCIYYsOPIHjjYM7FUDVmznOnQA";

  // Actor ID: uaa-user:df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  // Grant type: password
  // Client ID: credhub_cli
  // User ID: df0c1a26-2875-4bf5-baf9-716c6bb5ea6d
  public static final String EXPIRED_KEY_JWT = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiJlOWU1NzM5Y2QzODc0NDgzOGFjZjY4M2I3YWI0N2IwNCIsInN1YiI6ImRmMGMxYTI2LTI4NzUtNGJmNS1iYWY5LTcxNmM2YmI1ZWE2ZCIsInNjb3BlIjpbImNyZWRodWIud3JpdGUiLCJjcmVkaHViLnJlYWQiXSwiY2xpZW50X2lkIjoiY3JlZGh1Yl9jbGkiLCJjaWQiOiJjcmVkaHViX2NsaSIsImF6cCI6ImNyZWRodWJfY2xpIiwiZ3JhbnRfdHlwZSI6InBhc3N3b3JkIiwidXNlcl9pZCI6ImRmMGMxYTI2LTI4NzUtNGJmNS1iYWY5LTcxNmM2YmI1ZWE2ZCIsIm9yaWdpbiI6InVhYSIsInVzZXJfbmFtZSI6ImNyZWRodWJfY2xpIiwiZW1haWwiOiJjcmVkaHViX2NsaSIsImF1dGhfdGltZSI6MTQ5MDkwMzM1MywicmV2X3NpZyI6ImU0NDNkNzFlIiwiaWF0IjoxMDkwOTAzMzUzLCJleHAiOjEyOTA5MDMzNTQsImlzcyI6Imh0dHBzOi8vMTAuMjQ0LjAuMjo4NDQzL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNyZWRodWJfY2xpIiwiY3JlZGh1YiJdfQ.bilQiGpKcyFQwMHQ_yfG3ZQH735MpLakGnWwHACdSu0OhouILHOVFwshiUvWxp9UNR2R8Fftn-D34Wq4efYOV8_xd2qZoPwPCHPrY6rmyMPcvxlTXc_kOv0rXlLxqSs0-ENhD6S4vxmW8G1gfcLg_GsPC6NOh4tm9tOVvWtsGbSHxcUlvRrun3BueEkrDJTbs7tNFt8yLR6Wnpxm46QyeXo00c_tezKhCWeI_-AoZi8Ij3k3IyIsbIY6UFLeLqiSBtZ6_4NukEQdH1kLNyF55hWqwZx6k3Zio2UT9VHSOFokBZx46wL5t-_OtsuthN6ZlDPB6EmYfS0y-NtKxfCPfg";

  // Actor ID: uaa-user:9302d419-79e5-474d-ae4f-252206144db6
  // Grant type: password
  // Client ID: credhub_cli
  // User ID: 9302d419-79e5-474d-ae4f-252206144db6
  public static final String INVALID_JSON_JWT = "eyJhbGciOiJSUzI1NiJ9.ewogICJqdGkiOiAiNGE3ZjY0MWVkOGExNDI1Njk2NWQxYjNmYmFlNjcxNGUiLAogICJzdWIiOiAiOTMwMmQ0MTktNzllNS00NzRkLWFlNGYtMjUyMjA2MTQ0ZGI2IiwKICAic2NvcGUiOiBbCiAgICAiY3JlZGh1Yi53cml0ZSIsCiAgICAiY3JlZGh1Yi5yZWFkIgogIF0sCiAgImNsaWVudF9pZCI6ICJjcmVkaHViX2NsaSIsCiAgImNpZCI6ICJjcmVkaHViX2NsaSIsCiAgImF6cCI6ICJjcmVkaHViX2NsaSIsCiAgInJldm9jYWJsZSI6IHRydWUsCiAgImdyYW50X3R5cGUiOiAicGFzc3dvcmQiLAogICJ1c2VyX2lkIjogIjkzMDJkNDE5LTc5ZTUtNDc0ZC1hZTRmLTI1MjIwNjE0NGRiNiIsCiAgIm9yaWdpbiI6ICJ1YWEiLAogICJ1c2VyX25hbWUiOiAiY3JlZGh1YiIsCiAgImVtYWlsIjogImNyZWRodWIiLAogICJhdXRoX3RpbWUiOiAxNDkwODE4MzgwLAogICJyZXZfc2lnIjogIjgzOWFhZGQ3IiwKICAiaWF0IjogMTQ5MDgxODM4MCwKICAiZXhwIjogMTQ5MDkwNDc4MCwKICAiaXNzIjogImh0dHBzOi8vMzQuMjA2LjIzMy4xOTU6ODQ0My9vYXV0aC90b2tlbiIsCiAgInppZCI6ICJ1YWEiLAogICJhdWQiOiBbCiAgICAiY3JlZGh1Yl9jbGkiLAogICAgImNyZWRodWIiCiAgXQ.evNLKaZMjnDV_WXh1OMUOJo0SECTW7TMWCDtFcnVH3I_H4gg9oSazBKy2adL0sidaIQMxMohOTC9QmIvvog3k82BWcU6ZlWSqgim7Z5DG5gi0L5CLAyE-Bv3Z8A1O70xl62MIqgbET0VoVv7OL5ZyyM5AfCOwM1x7Itc5_Ee1zIiq1mIEl5QdeP4Wgaz-5DywnAAxguyM5sDFBPKtgfYRveu7lWlqOsp1Dy5L-WQ6jXzZWzMrK3zXBya_PCWSaoW06_whB5t3uoPWqWpZIJFcId5KkzQM5zOnu6JGFASNMMsZE0g2a0FdNzjIUuhiCT7dDgqh_woNNpKqhYSMXpMFQ";

  // Actor ID: uaa-user:1cc4972f-184c-4581-987b-85b7d97e909c
  // Grant type: password
  // Client ID: credhub
  // User ID: 1cc4972f-184c-4581-987b-85b7d97e909c
  public static final String INVALID_SIGNATURE_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJiOTc3NzIxNGI1ZDM0Zjc4YTJlMWMxZjZkYjJlYWE3YiIsInN1YiI6IjFjYzQ5NzJmLTE4NGMtNDU4MS05ODdiLTg1YjdkOTdlOTA5YyIsInNjb3BlIjpbImNyZWRodWIud3JpdGUiLCJjcmVkaHViLnJlYWQiXSwiY2xpZW50X2lkIjoiY3JlZGh1YiIsImNpZCI6ImNyZWRodWIiLCJhenAiOiJjcmVkaHViIiwiZ3JhbnRfdHlwZSI6InBhc3N3b3JkIiwidXNlcl9pZCI6IjFjYzQ5NzJmLTE4NGMtNDU4MS05ODdiLTg1YjdkOTdlOTA5YyIsIm9yaWdpbiI6InVhYSIsInVzZXJfbmFtZSI6ImNyZWRodWJfY2xpIiwiZW1haWwiOiJjcmVkaHViX2NsaSIsImF1dGhfdGltZSI6MTQ2OTA1MTcwNCwicmV2X3NpZyI6ImU1NGFiMzlhIiwiaWF0IjoxNDY5MDUxNzA0LCJleHAiOjM0NjkwNTE4MjQsImlzcyI6Imh0dHBzOi8vNTIuMjA0LjQ5LjEwNzo4NDQzL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNyZWRodWIiXX0.0CalandRHNK2t-0xQfK9EQ1UwW60V1d_jzCXxFOrt3dqq-y08Ri_LE8-gSACrcStGG7WU0o2yr5zJqe3z-NU-HG3G5HQPZx6WS3en_Sw-DjojzjyqjHEcNJ4IkRsQyHVQA3IZAx_ZMTdaf_oDRzf7QOwZ2PEE4vZct9GMQyVLns5rxPvbA1TSes-WlijxsTeO6CtbHoOSQU3Qfe8oF6P09Xazyy2dSs-geXglX_e6TUM-LBXrdXfJwz79lJ6rn99Y07enryY6yc0q7LX53c4JEaKh0kps_W0dsKxY4fV2ksKB31p4VSzk2bcHad7SITqypV-XraGYCFZMj00thuUhg";
}
