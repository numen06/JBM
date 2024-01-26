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

    @Test
    public void testRsa3() {
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg0yzo8J9kLgcS97JHIvUA1II2HKbWyqccX7jKLSWOx+NqPvIqO09TtgmciOxWJkGVFWaQLS/yyvvBGC+/5ao73Ul3GaDxUf4IQUiRNG/6qjZIaKY1iB7C83LwjdynQXSI0A5SqJW9loffcvvBTGFn8/WvXGUiBiz3BI+XJdhAKB3lunYGu2opP/plA9hf2koOFLU/VpSueb/8tQ84CxVuVmfkvTOtluY/hOTv2UEEkZqPQbs0rbcQG+wbIclhRsGa4joa1EpR3xo8rfwxgr++3QTPnzZghG/hwCKlDmWpTn6zr3xU4Bpw5PqYoFIbSr3eMfvfoSyZHXrcB2oEpkBJwIDAQAB";
        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCDTLOjwn2QuBxL3skci9QDUgjYcptbKpxxfuMotJY7H42o+8io7T1O2CZyI7FYmQZUVZpAtL/LK+8EYL7/lqjvdSXcZoPFR/ghBSJE0b/qqNkhopjWIHsLzcvCN3KdBdIjQDlKolb2Wh99y+8FMYWfz9a9cZSIGLPcEj5cl2EAoHeW6dga7aik/+mUD2F/aSg4UtT9WlK55v/y1DzgLFW5WZ+S9M62W5j+E5O/ZQQSRmo9BuzSttxAb7BshyWFGwZriOhrUSlHfGjyt/DGCv77dBM+fNmCEb+HAIqUOZalOfrOvfFTgGnDk+pigUhtKvd4x+9+hLJkdetwHagSmQEnAgMBAAECggEAEmaO7F3G3B+5M3bK53m/IiH+hFr3Y7zihNNcAWnhffKrlymGEwqAY4rORIO1CG4VoX8HqZbLJC2JhysJyWbzUO+xeSRSxjSSVQpTpaJhi4DTQS/4zFVr+RkDu7mqkc/thawr+0Yp+sdhj28jla49hEqL1SvUPYh8uFREMhSy7vPauxoxY8oLsERR1FWsOt43BEHyt3gdRkWeuSFbkx9UHJs/QuansyGMdmDFBoJcfJhyDqR18mWTKRPYejGTNBaV0bp5795f+38N5VZ4rZPK08SmXLsc9jyxj8+LWf1xK+ZfcY0B6efPOQHCe/tvE4KFLWzznj2WeWpv8Tq+VVIjmQKBgQC4sSQaZn/s3BgErkLv16IOGIo0BOOL3TQBr6EtaDQFTi08WYJyqyxx1AIFQq3j8ZHr98mT5la+OxIF+lOJNrAhmD+CxWJmcL6foqyLjunB/vxmKm8no6vd6affrf50zBXlJC8lWlUeSDCNqi6l4nVd1JAL/n+lwEHmNfqaYivkqQKBgQC1/kn1AWNPRCyVpniSeYmzcMw2ENf6LfdL1xPg8vZl92D1uJP4vOx20Eh0iySOIUOvz/aaYx0xRujAMy3fIJ01ECc4WEiZnkFvDH91UxnsmUEHAAcDintZYo8kE5J+67QC8AvynyksRPBJ3v+5lJoDulFrTl9BJ3W4nQZ7WpeJTwKBgCZje3OZVoGvBbcDWZpsoZx4cVF6qqlJoyOuk7vSau2nYDOpDzoyZhyKypi5UVeNwJzhdh6tw93qfUaMz4w3t8hj0t+lk1xjlturfl3LqL7P9zptG4mLtY98n44Ypkk0mmB0cvA6pHKU4rKvat7EAU0tf3tevL2BBrGJCrvIKANxAoGACELt2ohgmLWQjI6NBKEtxg6FxqKHG11Az5mI1npRAxnytdTWKsFwvof/8gJq+2S5LN7049dB+P2uDSOFno5ddI4yKZWgXhDYCZF4q7vE/n+KeyXeK6P6q3kU5F9bFBXhCw0Yjb/533+VpNTRqPv87dRkY8mf+1YuOt4t8CWRoXMCgYBSyah0IepUqbdgdniMhT0rhwl3rFtO3jUH8cPp0tYXGKaQhG/kdWzUMwCpRL2XbJyxviVJGC5jpXP31Zoit35yJQHm6Pj4TzrjHXtLkpcoLRCsKg7ThJiSzi6fLs5GXnGGzBkJlTEKNU7FLB5IfxOcdBWTQNhfhlaBKPjaGSVgNQ==";
        String key ="MQv7kRj84vaJBrv9FgBav5XI9gqPPUDLjndGE/IS2ZmCZNG8SGewLqh1vymJL3tEd+U/PHQIkmxD6rqVp6Nkz3aL7oArbq61EF2uSJeVIVck62fYMhDm1o7kUdYDvOBukcgv8da1phRZIYgdWPQo6FNPrtpIv+7bc8ODKnSl4pkfpZwPiqxZc94Aas8zsO2qnbUS58Jmrgkg/0V53WTVpEGR4Vy02AjL5CdwLqpmQNGigBXShLt+f55z9JoDLkBgx8DNfOz/0TLR6kWeguDNnsIk18z58s2Q2WbbPg1wZSxiExRzYVnx7KN/BldTv1D/4ET5d7oPkSVxszuETLaTvg==";

        RSA rsa = SecureUtil.rsa(privateKey, publicKey);
        String s = rsa.encryptBase64("Admin@123", KeyType.PublicKey);
        System.out.println(s);
        String sq2 = rsa.decryptStr(s, KeyType.PrivateKey);
        System.out.println(sq2);
        String sq = rsa.decryptStr(key, KeyType.PrivateKey);
        System.out.println(sq);

    }

    @Test
    public void testRsa4() {
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnwiJkDYoB/9lrwbMz/OyURIiALhJkzmyV9ESNc8h3Wf8eNFhp5h270CbkjR72M+rnc8DJOgPgDhxrOzM82hfXNvPnV4kw13F9/2KicVoWcWw1OMhjqNvDqiGhBTlKXcY/GE+5H5v/1R6ns5hQXMY6+fOXIpa9qMRVFJtDVQIWGLr06AE9Yjta4wRusjQf0jmSfuwPa8i8RltAahjrZzUpFd1PhX4ARMupmdyeak5aPrpgw9/EmERQJuY7+wSNORxZQMMx6U9Nb9f8cmE/ug2vbiiB1MIklLIJDz8iHcpBzQbbSGsJMJQI1zQw+yDSAsVNoOGTIbea0e7wh1bVW8+kQIDAQAB";
        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCfCImQNigH/2WvBszP87JREiIAuEmTObJX0RI1zyHdZ/x40WGnmHbvQJuSNHvYz6udzwMk6A+AOHGs7MzzaF9c28+dXiTDXcX3/YqJxWhZxbDU4yGOo28OqIaEFOUpdxj8YT7kfm//VHqezmFBcxjr585cilr2oxFUUm0NVAhYYuvToAT1iO1rjBG6yNB/SOZJ+7A9ryLxGW0BqGOtnNSkV3U+FfgBEy6mZ3J5qTlo+umDD38SYRFAm5jv7BI05HFlAwzHpT01v1/xyYT+6Da9uKIHUwiSUsgkPPyIdykHNBttIawkwlAjXNDD7INICxU2g4ZMht5rR7vCHVtVbz6RAgMBAAECggEAEPAT1qwt70wgRFfxEGTgJQpNXM5D585JVFkMG3cIIFCm7yc9w1/Vqv2ylAvK9PiJSrF1oESnAf0A/m3qFjKTlYxDvHr+Z2BhaVwzmSDRUF5hywq11D9L4L/KM98V2Si6HIcu758XxpX0yV0ptpLPoLB7pheGQKZ1fgmZW8YLbX5QSwvtMsfkM2+/wQqDkZuYeZvn+N9pI7ibxoMzbsDPv4jVyUOY7UnGKgc/04ed5ezi3jDPzPIogE42wWIXNvpf9vhz+gKiBPXRORjnYNHNaKLlIV9O1l3pSk93MxdIw4P4Pr3Kv8+SW7QeS9FlzydV0/wEUgn0UeKnK6frA75x0QKBgQDmgRjPxW0m6I0MDT2HYNoEZ5AFwZbglFfeRHEKjV8dOvwkgK4nXVkpt2C0HLuAZfSYx/2YOg9h47BAbyAQNuNQYpxssWcSNuPpEOiBOY/L77hd6uzWPfnf3OWN0+BUgTmHpfTU4AnQxpLYpBWWvZTQ5JsOWI/SmYRLsliKRjAvawKBgQCwn6/WBhxmTEbv78WBTWjKg6KfTn3/pLlcTg0sKZ8JaCljJc8DzpyyyUV3G/VyGMgVWWBQ2SCh7CaAkX1/rdhiPmNKLJfNtMb+CI7Xsjr+mACkMVynMRxUr3lpn6p+c50ikbMq+mckubWBAJpGc2Mow67y9oW0GINJ+vhb2tG08wKBgF3MmPkrwOA9b/BAjxJBElcKgawl/CzFkQAs38dIgjUV5LdB+hyguwDj06FXs+FrG970OzkwH0RZcEa4MdcHTvdOOZNtmaDq31HfhI3Zi6+UjHUDfX8tVzqG3I8bckVRFMUjfn8F8FiaBfXMx9BAs4dSD9sFpPkxaYmYOLmEGGWjAoGAL3ptmPnBjpo4g20PyYdWgQHLqElAQ4UzMqXkkpeJocy3O2jBckOLCeBi5PbGDuqsdli1NGZ5iC1j2DHD65t+zEH7DMhZ8jkDOOxyAXdutluD6J9ASc1A0V7uIz+BZEaN2eoSoJ35Nf0Z1likIAYKhL29fKBaJqRkPnHRa1eyAZMCgYBKRl/bRxB06+CubxppW1QvyC3NpK21kMnUwZzuAGgH6KqWQuYkTJOqlsuX9PArkYN9sTEJ42nhKvWBVez2lrPSL0C6RgDR6s+I/hVN9VW4F8aWIJStoTennPbS7vg9uGeTVBcpxYc3XzIx5iqtzkfjERWlKLfKiU64tmqEm1h0Aw==";
        String key ="MQv7kRj84vaJBrv9FgBav5XI9gqPPUDLjndGE/IS2ZmCZNG8SGewLqh1vymJL3tEd+U/PHQIkmxD6rqVp6Nkz3aL7oArbq61EF2uSJeVIVck62fYMhDm1o7kUdYDvOBukcgv8da1phRZIYgdWPQo6FNPrtpIv+7bc8ODKnSl4pkfpZwPiqxZc94Aas8zsO2qnbUS58Jmrgkg/0V53WTVpEGR4Vy02AjL5CdwLqpmQNGigBXShLt+f55z9JoDLkBgx8DNfOz/0TLR6kWeguDNnsIk18z58s2Q2WbbPg1wZSxiExRzYVnx7KN/BldTv1D/4ET5d7oPkSVxszuETLaTvg==";

        RSA rsa = SecureUtil.rsa(privateKey, publicKey);
        String s = rsa.encryptBase64("Admin@123", KeyType.PublicKey);
        System.out.println("加密内容");
        System.out.println(s);
        String sq2 = rsa.decryptStr(s, KeyType.PrivateKey);
        System.out.println("自己加密自己解密");
        System.out.println(sq2);
        String sq = rsa.decryptStr(key, KeyType.PrivateKey);
        System.out.println("输出内容的解密");
        System.out.println(sq);

    }
}
