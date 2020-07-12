package com.instagramclone.Model

class ChatList {

    private var id: String = ""

    fun getId(): String {
        return id
    }

    fun setId(id: String) {
        this.id = id
    }

    constructor()

    constructor(id: String) {
        this.id = id
    }

}