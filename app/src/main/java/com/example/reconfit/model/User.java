package com.example.reconfit.model;

import com.google.firebase.Timestamp;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    public String id;
    public String name;
    public String lastName1;
    public String lastName2;
    public String email;
    public String password;
    public String fecnac;
    public String genero;
    public Timestamp fecreg;
}
