package edu.cmu.cc.webtier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestDrive {

  public static void main() throws Exception {
    String testStr = "eJyFkNFqwzAMRf9Fz36QbMu28iujFCdVm0CbjSa0g5J_n1PMsnUP05O5V-Ye3Qd0fR5GaN4ekM_n_fz5fF61u0FDKDF5j2jRiYF8maFhrGNgHi4KDRAnyxY9bgMG-jz1xbSibRedwLIz8PF-L5J3xR4O0OD3FpKkI0ctxpyvJ51XTWExv6Cm4VSYXOKUAllL3psKGgN7ocAJyRk4asHySZjYV2oXUnRuPeIFmjgm5iTIvoRPOh7-nF0ZqZMuaHZPqhprJQhHizHSf_WISMCSYgP6rR5ER5Kd_qiHqNZD21ZUbjm3r_WUP6Pe95smUJW1sN3yBfIbe98=";
    String resultStr = "eJyF0cFOwzAMBuB38TkHO46TuK-C0JS23lZpG2irAGnqu5NOhTEYkFMUR_Ln32fotmU4QPNwhrLbrca3y_Vo3Qs0hJpyCIgeWR2U_QiN4HIcjMPeoAGS7MVjwOsBB9ty2taiV2u7xArTo4Pnp9f6FLiWhx4a_PyFpHktyWphLMeNjfObweRuUKdhU02cJedI3lMIboGmKEEpSkZiB2urrJBVSMKi5pgT8zzENzRJyiJZUUJtfrJD_2PsxUiddtEKX1RLW69RJXlMif6LR1Uj1i4-YrjGg8ikhe1LPERLPHT9lUxaKe3f8fy-My_3USR3UevYJ-tKf0HNEn-zug9UYGPCeIOa9zy9A8u9mP4A";
    JsonObject requestJson = UtilsBlock.decompress(testStr);
    JsonObject resultJson = UtilsBlock.decompress(resultStr);
    System.out.print("invalid POW: \n");
    System.out.print(requestJson);
    System.out.print("\n");
    System.out.print(resultJson);
  }
}