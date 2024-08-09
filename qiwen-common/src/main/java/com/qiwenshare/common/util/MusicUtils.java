package com.qiwenshare.common.util;

import com.alibaba.fastjson2.JSON;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (StringUtils.isNotEmpty(albumName)) {
            String s = HttpsUtils.doGetString(
                    "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?_=1655481018175&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&hostUin=0&is_xml=0&key=" + albumName,
                    headMap);
            Map map = JSON.parseObject(s, Map.class);
            Map data = (Map) map.get("data");
            Map album = (Map) data.get("album");
            List<Map> albumlist = (List<Map>) album.get("itemlist");
            for (Map item : albumlist) {
                String albumItem = (String) item.get("name");
                singer = (String) item.get("singer");
                if (albumName.equals(albumItem) && singerName.equals(singer)) {
                    mid = (String) item.get("mid");
                }
            }

            // 尝试从专辑信息中获取歌曲信息
            if (StringUtils.isNotEmpty(mid)) {
                s = HttpsUtils.doGetString(
                        "https://c.y.qq.com/v8/fcg-bin/musicmall.fcg?_=1655481477830&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" +
                                ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&cmd=get_album_buy_page&albummid=" + mid + "&albumid=0",
                        headMap);
                map = JSON.parseObject(s, Map.class);
                data = (Map) map.get("data");
                List<Map> songlist = (List<Map>) data.get("songlist");
                for (Map item : songlist) {
                    String songname = (String) item.get("songname");
                    String songorig = (String) item.get("songorig");

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
            String s = HttpsUtils.doGetString(
                    "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?_=1651992748984&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&hostUin=0&is_xml=0&key=" + mp3Name.replaceAll(
                            " ", ""), headMap);
            Map map = JSON.parseObject(s, Map.class);
            Map data = (Map) map.get("data");
            Map song = (Map) data.get("song");
            List<Map> list = (List<Map>) song.get("itemlist");

            for (Map item : list) {
                singer = (String) item.get("singer");
                id = (String) item.get("id");
                mid = (String) item.get("mid");
                try {
                    String singer1 = PinyinHelper.convertToPinyinString(singerName.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                    String singer2 = PinyinHelper.convertToPinyinString(singer.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                    if (singer1.contains(singer2) || singer2.contains(singer1)) {
                        isMatch = true;
                        break;
                    }
                }
                catch (PinyinException e) {
                    e.printStackTrace();
                }
            }

            if (!isMatch) {
                for (Map item : list) {
                    singer = (String) item.get("singer");
                    id = String.valueOf(item.get("id"));
                    mid = String.valueOf(item.get("mid"));
                    try {
                        String singer2 = PinyinHelper.convertToPinyinString(singer.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                        String singer3 = PinyinHelper.convertToPinyinString(mp3Name.replaceAll(" ", ""), ",", PinyinFormat.WITHOUT_TONE);
                        if (singer3.contains(singer2) || singer2.contains(singer3)) {
                            isMatch = true;
                            break;
                        }
                    }
                    catch (PinyinException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!isMatch) {
                Map album = (Map) data.get("album");
                List<Map> albumlist = (List<Map>) album.get("itemlist");
                for (Map item : albumlist) {
                    String mp3name = (String) item.get("name");
                    singer = (String) item.get("singer");
                    id = (String) item.get("id");
                    mid = (String) item.get("mid");
                    if (singer.equals(singerName) && mp3Name.equals(mp3name)) {
                        String res = HttpsUtils.doGetString(
                                "https://c.y.qq.com/v8/fcg-bin/musicmall.fcg?_=1652026128283&cv=4747474&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&notice=0&platform=yqq" + ".json&needNewCode=1&uin=0&g_tk_new_20200303=5381&g_tk=5381&cmd=get_album_buy_page&albummid=" + mid + "&albumid=0",
                                headMap);
                        Map map1 = JSON.parseObject(res, Map.class);
                        Map data1 = (Map) map1.get("data");
                        List<Map> list1 = (List<Map>) data1.get("songlist");
                        for (Map item1 : list1) {
                            if (mp3Name.equals((String) item1.get("songname"))) {
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
