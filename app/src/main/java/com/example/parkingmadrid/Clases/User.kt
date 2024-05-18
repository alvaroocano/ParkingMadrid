package com.example.parkingmadrid.Clases

class User {
    var dateOfBirth: String? = null
    var fullName: String? = null
    var nickname: String? = null

    constructor()
    constructor(dateOfBirth: String?, fullName: String?) {
        this.dateOfBirth = dateOfBirth
        this.fullName = fullName
    }

    constructor(dateOfBirth: String?, fullName: String?, nickname: String?) {
        this.dateOfBirth = dateOfBirth
        this.fullName = fullName
        this.nickname = nickname
    }
}
