package com.jbm.test.token;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

/**
 * @Created wesley.zhang
 * @Date 2022/6/14 15:51
 * @Description TODO
 */
public class KeyRsaTest {

    @Test
    public void testRsa() {
//        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDG03gR1w6i3E6h6+N9F2///BnRrkzPc7RT4qZKKl2b/rolym0EYl3QZTsIV5oQngT93TLtld7EK5svdwUabX6kzqd8yDDChZXS/E7/FrufN6Hwf9S3O3ZzkhEyd45HmRHV4aNRFsS/NviEZx83D6FR94l0SPnomvPkVqM8UnafnQIDAQAB";
//        String privateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAMbTeBHXDqLcTqHr430Xb//8GdGuTM9ztFPipkoqXZv+uiXKbQRiXdBlOwhXmhCeBP3dMu2V3sQrmy93BRptfqTOp3zIMMKFldL8Tv8Wu583ofB/1Lc7dnOSETJ3jkeZEdXho1EWxL82+IRnHzcPoVH3iXRI+eia8+RWozxSdp+dAgMBAAECgYAJjtfqT6LR/HJBQXQ9qrdFIIrjNBRYMrE8CRzCWvgGDEBJmcoU2F+3KW6lj4SGAPqvc4dDuZ0sZAZBSWDy7MmWL+Zz2z44sulxsOsb3DJqIyBSAr5D6mhrRmu7MJA5AGgDHo/2gn+9Cji2JQBHBFe18BzJdr2tIM4uAYTVB6EW8QJBAPCrnHohSDtgLSmHrbORP/cIS8OOF/M3PsYfHZ3cpdrKk2zs1rXAHJq80GlmhSQx8tezx6wt63Cph0reiHbOMRkCQQDTfYqahFR0NTFFfTBfSJKQEqoiRYMnOrjkkOOgFv6cBwYd16pnqTfNISSYkBsOcDO09qiMILW96MoJONCV458lAkEAmMrqueK9X+zMX0xjK9hwOp5Ks2lXrTKKqO+CNwGpTkFD3WhzW8oOnvJ2giPzLSqE2QqrHpW8nrcSTKcBDiQTqQJABORmjGR7P6TrWtwmfk3Ddim4XcqV2hZ1qHPhkBZ4FUvkTFRs0LENZWVa31yWA6N8zrbV90fabGYyJjx2NsFpMQJARtRflzJjWc/49nzu+om41bz9Ngg07/S8Rxe8AlZbSlCxggmp/KUBcoVgNJCa5qGsX2AvTOCXaHngp+YLtHHPBQ==";
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048);
        String publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        String privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
        RSA rsa = SecureUtil.rsa(privateKey, publicKey);
        String s = rsa.encryptBase64("nishishui".getBytes(), KeyType.PublicKey);
        System.out.println(s);
        String sq = rsa.decryptStr(s, KeyType.PrivateKey);
        System.out.println(sq);
    }


    @Test
    public void testRsa2() {
//        String publicKey = "7gBZcbsC7kLIWCdELIl8nxcs";
//        String privateKey = "0osTIhce7uPvDKHz6aa67bhCukaKoYl4";
        String seed = "0osTIhce7uPvDKHz6aa67bhCukaKoYl4";
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 1024, "0osTIhce7uPvDKHz6aa67bhCukaKoYl4".getBytes());
        String publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        String privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
        System.out.println(publicKey);
        System.out.println(privateKey);
        RSA rsa = SecureUtil.rsa(privateKey, publicKey);
        String s = rsa.encryptBase64("nishishui".getBytes(), KeyType.PublicKey);
        System.out.println(s);
        String sq = rsa.decryptStr(s, KeyType.PrivateKey);
        System.out.println(sq);
    }
}
