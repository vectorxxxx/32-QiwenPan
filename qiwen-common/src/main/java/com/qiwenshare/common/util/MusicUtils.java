package com.qiwenshare.common.util;

import com.alibaba.fastjson2.JSON;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MusicUtils
{

    /**
     * 根据输入的MP3名称和歌手名称，获取歌曲的歌词
     *
     * @param singerName 歌手姓名
     * @param mp3Name    MP3 名称
     * @param albumName  专辑名称
     * @return {@link String }
     */
    public static String getLyc(String singerName, String mp3Name, String albumName) {
        Map<String, Object> headMap = new HashMap<>();
        headMap.put("Referer", "https://y.qq.com/");
        String singer = "";
        String id = "";
        String mid = "";
        boolean isMatch = false;
        // 检查专辑名称是否非空
        if (StringUtils.isNotEmpty(albumName)) {
            // 构建并发送HTTPS请求，获取专辑信息
            String s = HttpsUtils.doGetString(
                    "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?_=1655481018175&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&hostUin=0&is_xml=0&key=" + albumName,
                    headMap);
            // 解析返回的JSON数据为Map对象
            Map map = JSON.parseObject(s, Map.class);
            // 获取返回数据中的专辑信息
            Map data = (Map) map.get("data");
            Map album = (Map) data.get("album");
            List<Map> albumlist = (List<Map>) album.get("itemlist");
            // 遍历专辑列表，寻找匹配的专辑和歌手
            for (Map item : albumlist) {
                String albumItem = (String) item.get("name");
                singer = (String) item.get("singer");
                // 当专辑名称和歌手名称匹配时，获取歌曲的mid
                if (albumName.equals(albumItem) && singerName.equals(singer)) {
                    mid = (String) item.get("mid");
                }
            }

            // 尝试从专辑信息中获取歌曲信息
            if (StringUtils.isNotEmpty(mid)) {
                // 构建并发送HTTPS请求，获取专辑内的歌曲列表
                s = HttpsUtils.doGetString(
                        "https://c.y.qq.com/v8/fcg-bin/musicmall.fcg?_=1655481477830&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" +
                                ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&cmd=get_album_buy_page&albummid=" + mid + "&albumid=0",
                        headMap);
                // 解析返回的JSON数据为Map对象
                map = JSON.parseObject(s, Map.class);
                data = (Map) map.get("data");
                List<Map> songlist = (List<Map>) data.get("songlist");
                // 遍历歌曲列表，寻找匹配的歌曲
                for (Map item : songlist) {
                    String songname = (String) item.get("songname");
                    String songorig = (String) item.get("songorig");
                    // 当歌曲名称或原始歌曲名称匹配时，获取歌曲的mid和id，并设置匹配标志为true
                    if (mp3Name.equals(songname) || mp3Name.equals(songorig)) {
                        mid = (String) item.get("songmid");
                        id = String.valueOf(item.get("songid"));
                        isMatch = true;
                    }
                }
            }

        }

        // 如果未从专辑信息中获取到，尝试从智能盒中获取歌曲信息
        if (!isMatch) {
            // 发送请求获取歌曲信息
            String s = HttpsUtils.doGetString(
                    "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?_=1651992748984&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&hostUin=0&is_xml=0&key=" + mp3Name.replaceAll(
                            " ", ""), headMap);
            // 解析响应的JSON数据
            Map map = JSON.parseObject(s, Map.class);
            Map data = (Map) map.get("data");
            Map song = (Map) data.get("song");
            List<Map> list = (List<Map>) song.get("itemlist");
            // 第一次匹配尝试：比较歌手名称
            for (Map item : list) {
                singer = (String) item.get("singer");
                id = (String) item.get("id");
                mid = (String) item.get("mid");
                try {
                    // 将歌手名称转换为拼音进行匹配
                    String singer1 = PinyinHelper.convertToPinyinString(singerName.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                    String singer2 = PinyinHelper.convertToPinyinString(singer.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                    // 检查是否部分匹配
                    if (singer1.contains(singer2) || singer2.contains(singer1)) {
                        isMatch = true;
                        break;
                    }
                }
                catch (PinyinException e) {
                    log.error("匹配出错：{}", e.getMessage(), e);
                }
            }

            // 第二次匹配尝试：比较歌曲名称
            if (!isMatch) {
                for (Map item : list) {
                    singer = (String) item.get("singer");
                    id = String.valueOf(item.get("id"));
                    mid = String.valueOf(item.get("mid"));
                    try {
                        // 将歌曲名称转换为拼音进行匹配
                        String singer2 = PinyinHelper.convertToPinyinString(singer.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                        String singer3 = PinyinHelper.convertToPinyinString(mp3Name.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                        // 检查是否部分匹配
                        if (singer3.contains(singer2) || singer2.contains(singer3)) {
                            isMatch = true;
                            break;
                        }
                    }
                    catch (PinyinException e) {
                        log.error("匹配出错：{}", e.getMessage(), e);
                    }
                }
            }

            // 第三次尝试：从专辑中提取歌曲信息
            if (!isMatch) {
                Map album = (Map) data.get("album");
                List<Map> albumlist = (List<Map>) album.get("itemlist");
                for (Map item : albumlist) {
                    String mp3name = (String) item.get("name");
                    singer = (String) item.get("singer");
                    id = (String) item.get("id");
                    mid = (String) item.get("mid");
                    // 检查歌曲名称和歌手名称是否完全匹配
                    if (singer.equals(singerName) && mp3Name.equals(mp3name)) {
                        // 获取关于专辑的额外数据
                        String res = HttpsUtils.doGetString(
                                "https://c.y.qq.com/v8/fcg-bin/musicmall.fcg?_=1652026128283&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&cmd=get_album_buy_page&albummid=" + mid + "&albumid=0",
                                headMap);
                        // 解析专辑响应
                        Map map1 = JSON.parseObject(res, Map.class);
                        Map data1 = (Map) map1.get("data");
                        List<Map> list1 = (List<Map>) data1.get("songlist");
                        for (Map item1 : list1) {
                            // 检查专辑中的歌曲名称是否匹配
                            if (mp3Name.equals(item1.get("songname"))) {
                                id = String.valueOf(item1.get("songid"));
                                mid = String.valueOf(item1.get("songmid"));
                                isMatch = true;
                                break;
                            }
                        }
                        if (isMatch) {
                            break;
                        }

                    }
                }
            }
        }

        // 根据获取的歌曲信息，请求并返回歌词
        String s1 = HttpsUtils.doGetString(
                "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?_=1651993218842&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&loginUin=0&" + "songmid=" + mid + "&" + "musicid=" + id,
                headMap);
        Map map1 = JSON.parseObject(s1, Map.class);
        return (String) map1.get("lyric");
    }

    //    public static void main(String[] args) {
    //        String lyc = getLyc("周杰伦", "以父之名", "周杰伦地表最强世界巡回演唱会");
    //        String decodeStr = Base64.decodeStr(lyc);
    //        System.out.println(decodeStr);
    //    }
}
