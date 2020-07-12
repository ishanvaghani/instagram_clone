package com.instagramclone.Model

class Post {
    private var postId: String = ""
    private var image: String = ""
    private var publisher: String = ""
    private var description: String = ""

    constructor()

    constructor(postId: String, image: String, publisher: String, description: String) {
        this.postId = postId
        this.image = image
        this.publisher = publisher
        this.description = description
    }

    fun getPostId(): String {
        return postId
    }

    fun setPostId(postId: String) {
        this.postId = postId
    }

    fun getImage(): String {
        return image
    }

    fun setImage(image: String) {
        this.image = image
    }

    fun getPublisher(): String {
        return publisher
    }

    fun setPublisher(publisher: String) {
        this.publisher = publisher
    }

    fun getDescription(): String {
        return description
    }

    fun setDescription(description: String) {
        this.description = description
    }
}