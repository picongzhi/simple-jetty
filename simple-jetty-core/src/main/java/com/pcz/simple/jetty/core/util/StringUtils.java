package com.pcz.simple.jetty.core.util;

import java.nio.charset.StandardCharsets;

/**
 * 字符串工具类
 *
 * @author picongzhi
 */
public class StringUtils {
    /**
     * Ascii 小写字符表
     */
    private static final char[] LOWER_CASES =
            {
                    '\000', '\001', '\002', '\003', '\004', '\005', '\006', '\007',
                    '\010', '\011', '\012', '\013', '\014', '\015', '\016', '\017',
                    '\020', '\021', '\022', '\023', '\024', '\025', '\026', '\027',
                    '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037',
                    '\040', '\041', '\042', '\043', '\044', '\045', '\046', '\047',
                    '\050', '\051', '\052', '\053', '\054', '\055', '\056', '\057',
                    '\060', '\061', '\062', '\063', '\064', '\065', '\066', '\067',
                    '\070', '\071', '\072', '\073', '\074', '\075', '\076', '\077',
                    '\100', '\141', '\142', '\143', '\144', '\145', '\146', '\147',
                    '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157',
                    '\160', '\161', '\162', '\163', '\164', '\165', '\166', '\167',
                    '\170', '\171', '\172', '\133', '\134', '\135', '\136', '\137',
                    '\140', '\141', '\142', '\143', '\144', '\145', '\146', '\147',
                    '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157',
                    '\160', '\161', '\162', '\163', '\164', '\165', '\166', '\167',
                    '\170', '\171', '\172', '\173', '\174', '\175', '\176', '\177'
            };


    /**
     * 判断给定的字符串是不是空字符串（为 null 或 只有空字符）
     *
     * @param str 字符串
     * @return 是不是空字符串
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }

        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.codePointAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取字节数组
     *
     * @param str 字符串
     * @return 字节数组
     */
    public static byte[] getBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Ascii 转 小写
     *
     * @param str 字符串
     * @return 小写字符串
     */
    public static String asciiToLowerCase(String str) {
        if (str == null) {
            return null;
        }

        char[] chars = null;
        int i = str.length();

        // 找到第一个大写字符
        while (i-- > 0) {
            char ch = str.charAt(i);
            if (ch < 127) {
                char lowerCh = LOWER_CASES[ch];
                if (ch != lowerCh) {
                    chars = str.toCharArray();
                    chars[i] = lowerCh;
                    break;
                }
            }
        }

        while (i-- > 0) {
            if (chars[i] <= 127) {
                chars[i] = LOWER_CASES[chars[i]];
            }
        }

        return chars == null ? str : new String(chars);
    }
}
