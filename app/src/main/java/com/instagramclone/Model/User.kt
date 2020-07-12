package com.instagramclone.Model

class User {
    private var fullname: String = ""
    private var username: String = ""
    private var bio: String = ""
    private var uid: String = ""
    private var image: String = ""
    private var typingTo: String = ""
    private var status: String = ""
    private var search: String = ""

    constructor()

    constructor(
        fullname: String,
        username: String,
        bio: String,
        uid: String,
        image: String,
        typingTo: String,
        status: String,
        search: String
    ) {
        this.fullname = fullname
        this.username = username
        this.bio = bio
        this.uid = uid
        this.image = image
        this.typingTo = typingTo
        this.status = status
        this.search = search
    }

    fun getUsername(): String {
        return username
    }

    fun setUsername(username: String) {
        this.username = username
    }

    fun getFullname(): String {
        return fullname
    }

    fun setFullname(fullname: String) {
        this.fullname = fullname
    }

    fun getBio(): String {
        return bio
    }

    fun setBio(bio: String) {
        this.bio = bio
    }

    fun getImage(): String {
        return image
    }

    fun setImage(image: String) {
        this.image = image
    }

    fun getUid(): String {
        return uid
    }

    fun setUid(uid: String) {
        this.uid = uid
    }

    fun getTypingTo(): String {
        return typingTo
    }

    fun setTypingTo(typingTo: String) {
        this.typingTo = typingTo
    }

    fun getStatus(): String {
        return status
    }

    fun setStatus(status: String) {
        this.status = status
    }

    fun getSearch(): String {
        return search
    }

    fun setSearch(search: String) {
        this.search = search
    }
}