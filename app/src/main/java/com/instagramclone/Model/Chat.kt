package com.instagramclone.Model

class Chat {
    private var sender: String = ""
    private var receiver: String = ""
    private var message: String = ""
    private var isseen = false
    private var msgid: String = ""
    private var time: String = ""

    constructor()

    constructor(
        sender: String,
        receiver: String,
        message: String,
        isseen: Boolean,
        msgid: String,
        time: String
    ) {
        this.sender = sender
        this.receiver = receiver
        this.message = message
        this.isseen = isseen
        this.msgid = msgid
        this.time = time
    }


    fun getTime(): String {
        return time
    }

    fun setTime(time: String) {
        this.time = time
    }

    fun getSender(): String {
        return sender
    }

    fun setSender(sender: String) {
        this.sender = sender
    }

    fun getReceiver(): String {
        return receiver
    }

    fun setReceiver(receiver: String) {
        this.receiver = receiver
    }

    fun getMessage(): String {
        return message
    }

    fun setMessage(message: String) {
        this.message = message
    }

    fun isIsseen(): Boolean {
        return isseen
    }

    fun setIsseen(isseen: Boolean) {
        this.isseen = isseen
    }

    fun getMsgid(): String? {
        return msgid
    }

    fun setMsgid(msgid: String) {
        this.msgid = msgid
    }

}