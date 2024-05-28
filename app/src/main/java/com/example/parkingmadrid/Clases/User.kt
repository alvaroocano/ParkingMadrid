package com.example.parkingmadrid.Clases

class User {
    var dateOfBirth: String? = null
    var fullName: String? = null
    var nickname: String? = null
    var email: String? = null
    var profileImage: String? = null

    constructor()

    constructor(dateOfBirth: String?, fullName: String?, nickname: String?, email: String?, profileImage: String?) {
        this.dateOfBirth = dateOfBirth
        this.fullName = fullName
        this.nickname = nickname
        this.email = email
        this.profileImage = profileImage
    }

    constructor(dateOfBirth: String?, fullName: String?, nickname: String?, email: String?) {
        this.dateOfBirth = dateOfBirth
        this.fullName = fullName
        this.nickname = nickname
        this.email = email
    }
}
