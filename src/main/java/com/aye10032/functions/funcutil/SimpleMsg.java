package com.aye10032.functions.funcutil;

import com.dazo66.command.interfaces.ICommand;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;


/**
 * CQMsg的包装对象 每个传入的消息都进行包装后交由模块执行
 *
 * @author Dazo66
 */
public class SimpleMsg implements ICommand {

    private long fromGroup = -1;
    private long fromClient = -1;
    private String msg;
    private MsgType type;
    private MessageEvent event;
    private MessageSource source;

    public SimpleMsg(long fromGroup, long fromClient, String msg, MsgType type) {
        this.fromGroup = fromGroup;
        this.fromClient = fromClient;
        this.msg = msg;
        this.type = type;
    }

    public SimpleMsg(MessageEvent event) {
        if (event instanceof GroupMessageEvent) {
            type = MsgType.GROUP_MSG;
            fromGroup = ((GroupMessageEvent) event).getGroup().getId();
        } else if (event instanceof FriendMessageEvent) {
            type = MsgType.PRIVATE_MSG;
        } else if (event instanceof GroupTempMessageEvent) {
            type = MsgType.PRIVATE_MSG;
        }
        fromClient = event.getSender().getId();
        msg = getMsgFromEvent(event);
        this.event = event;
    }

    private String getMsgFromEvent(MessageEvent event) {
        MessageChain chain = event.getMessage();
        MessageChainBuilder builder = new MessageChainBuilder();
        chain.forEach((Message m) -> {
            if (m instanceof At) {
                builder.add(m);
            } else if (m instanceof MessageSource) {
                source = (MessageSource) m;
            } else {
                builder.add(m);
            }
        });
        return builder.build().toString();
    }

    /**
     * 获取消息从哪个群收到的
     *
     * @return 群id 从私聊消息或者ts消息则为-1
     */
    public long getFromGroup() {
        return fromGroup;
    }

    /**
     * 设置消息的群来源
     * @param fromGroup 群消息来源
     */
    public void setFromGroup(long fromGroup) {
        this.fromGroup = fromGroup;
    }

    /**
     * 获取消息是哪个人发送的
     * @return 发送者id qq消息则是发送者qq号
     */
    public long getFromClient() {
        return fromClient;
    }

    /**
     * 设置发送者id
     * @param fromClient 发送者id qq消息则是发送者qq号
     */
    public void setFromClient(long fromClient) {
        this.fromClient = fromClient;
    }

    /**
     * 获得消息内容体
     * @return 消息内容
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置消息内容 一般不推荐在func内部设置
     * 可以通过设置这个为空 让这个消息只被短路处理
     * @param msg 消息内容
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 消息类型 具体消息类型如下 {@link MsgType}
     * @return 当前的消息类型
     */
    public MsgType getType() {
        return type;
    }

    /**
     * 设置消息类型 一般不允许设置 如果要自定义返回途径 可以通过new 这个对象
     * @param type 消息类型
     */
    public void setType(MsgType type) {
        this.type = type;
    }

    /**
     * 是否是qq群消息
     * @return true or false
     */
    public boolean isGroupMsg(){
        return type == MsgType.GROUP_MSG;
    }

    /**
     * 是否是qq私聊消息
     * @return true or false
     */
    public boolean isPrivateMsg(){
        return type == MsgType.PRIVATE_MSG;
    }

    /**
     * 是否是ts消息
     * @return true or false
     */
    public boolean isTeamspealMsg(){
        return type == MsgType.TEAMSPEAK_MSG;
    }

    /**
     * 消息的分片方法 推荐使用这个 在获取消息的时候 会自动替换掉多空格 再分片
     * 也是用于Commander的方法
     * @return 消息分片后的数组
     */
    @Override
    public String[] getCommandPieces() {
        return msg.trim().replaceAll(" +", " ").split(" ");
    }

    /**
     * 生成一个测试用的CQmsg对象
     *
     * @param testMsg
     * @return
     */
    public static SimpleMsg getTempMsg(String testMsg){
        return new SimpleMsg(995497677L, 2375985957L, testMsg, MsgType.GROUP_MSG);
    }
}
