package com.aye10032.timetask;

import com.aye10032.Zibenbot;
import com.aye10032.utils.ExceptionUtils;
import com.aye10032.utils.timeutil.Reciver;
import com.aye10032.utils.timeutil.SubscribableBase;
import com.aye10032.utils.timeutil.TimeUtils;
import com.aye10032.utils.weibo.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @author Dazo66
 */
public abstract class ArknightWeiboTask extends SubscribableBase {

    private Set<String> postIds = new HashSet<>();
    private WeiboReader weiboReader;
    private static JsonParser parser = new JsonParser();
    private WeiboSetItem offAnnounce = null;

    public ArknightWeiboTask(Zibenbot zibenbot, WeiboReader reader) {
        super(zibenbot);
        weiboReader = reader;
        File file = new File(getBot().appDirectory + "\\arknight\\");
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public Date getNextTime(Date date) {
        Date ret = new Date();
        ret.setTime(date.getTime() + TimeUtils.MIN * 3L);
        Calendar c = Calendar.getInstance();
        c.setTime(ret);
        c.set(Calendar.SECOND, 1);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (hour < 8 || hour > 22) {
            c.set(Calendar.HOUR_OF_DAY, 8);
            c.set(Calendar.MINUTE, 5);
            if (hour >= 8) {
                c.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return c.getTime();
    }

    @Override
    public void run(List<Reciver> recivers, String[] args) {
        OkHttpClient client = Zibenbot.getOkHttpClient();
        WeiboSet posts = WeiboUtils.getWeiboSet(client, 6279793937L);
        if (postIds.isEmpty()) {
            posts.forEach(post -> postIds.add(post.getId()));
            try {
                offAnnounce = getPostUrlFromOff();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Iterator<WeiboSetItem> postIterator = posts.iterator();
            while (postIterator.hasNext()) {
                WeiboSetItem post = postIterator.next();
                if (postIds.contains(post.getId())) {
                    postIterator.remove();
                } else {
                    postIds.add(post.getId());
                    if (!post.isPerma()) {
                        try {
                            getBot().logInfo(String.format("检测到方舟新的饼：%s", post.getTitle()));
                            replyAll(recivers, weiboReader.postToUser(WeiboUtils.getWeiboWithPostItem(client, post)));
                        } catch (Exception e) {
                            getBot().logWarning("获取饼出错：" + ExceptionUtils.printStack(e));
                        }
                    }
                }
            }
        }
        try {
            WeiboSetItem item = getPostUrlFromOff();
            if (offAnnounce != item && item != null && !item.equals(offAnnounce)) {
                offAnnounce = item;
                StringBuilder ret = new StringBuilder();
                ret.append(item.getTitle()).append("\n");
                ret.append(weiboReader.postToUser(getPostFromOff(offAnnounce)));
                replyAll(recivers, ret.toString());
                getBot().logInfo(String.format("检测到方舟新的制作组通讯（来自官网）：%s", item.getTitle()));
            } else if (offAnnounce != item && item == null) {
                offAnnounce = null;
            }
        } catch (Exception e) {
            getBot().logWarning("读取方舟制作组通讯出错：" + ExceptionUtils.printStack(e));
        }
    }

    public static WeiboPost getPostFromOff(WeiboSetItem item) throws IOException {
        if (item.isOffAnnounce()) {
            OkHttpClient client = Zibenbot.getOkHttpClient();
            Request officialWebsiteRequest = new Request.Builder()
                    .url(item.getId())
                    .method("GET", null)
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                    .build();
            String offWeb = client.newCall(officialWebsiteRequest).execute().body().string();
            WeiboPost post = new WeiboPost();
            post.setUserId("-1");
            post.setTitle(item.getTitle());
            post.setUserName("明日方舟Arknights");
            post.setLink(item.getId());
            post.setPermaLink(post.getLink());
            post.setDescription(post.getTitle() + "\n" + cleanDes(offWeb));
            return post;
        }
        return null;
    }

    public static String cleanDes(String offWeb) {
        offWeb = offWeb.replaceAll("<span class=\"head-title\">\\s*([\\S\\s]+)\\s*</span>", "");
        offWeb = offWeb.replaceAll("<img src=\"(\\S+)\"/>", "[img:$1]\n");
        offWeb = offWeb.replaceAll("<title>公告</title>", "");
        offWeb = offWeb.replaceAll("\n", "");
        offWeb = offWeb.replaceAll("\t", "");
        offWeb = offWeb.replaceAll("<br/>", "\n");
        offWeb = offWeb.replaceAll("</p>", "\n");
        offWeb = offWeb.replaceAll("<[\\S\\s]+?>", "");
        offWeb = offWeb.replaceAll("\n\n\n", "\n\n");
        offWeb = offWeb.trim();
        return offWeb;
    }

    public static WeiboSetItem getPostUrlFromOff() throws IOException {
        OkHttpClient client = Zibenbot.getOkHttpClient();
        Request officialWebsiteRequest = new Request.Builder()
                .url("https://ak-fs.hypergryph.com/announce/Android/announcement.meta.json")
                .method("GET", null)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .build();
        String offWeb = client.newCall(officialWebsiteRequest).execute().body().string();
        JsonObject offWebJson = parser.parse(offWeb).getAsJsonObject();
        for (JsonElement jsonElement : offWebJson.getAsJsonArray("announceList")) {
            JsonObject object1 = jsonElement.getAsJsonObject();
            if (object1.get("title").getAsString().contains("制作组通讯")) {
                WeiboSetItem item = new WeiboSetItem();
                item.setOffAnnounce(true);
                item.setPerma(false);
                String temp = object1.get("webUrl").toString();
                item.setId(clean(object1.get("webUrl").toString()));
                item.setTitle(clean(object1.get("title").toString()));
                return item;
            }
        }
        return null;
    }

    private static String clean(String s) {
        if (s.startsWith("\"")) {
            return s.replaceAll("\"", "");
        }
        return s;
    }


}
