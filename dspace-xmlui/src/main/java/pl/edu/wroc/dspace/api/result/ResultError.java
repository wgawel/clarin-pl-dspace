/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.wroc.dspace.api.result;

import com.google.gson.Gson;

/**
 *
 * @author wgawel
 */
public class ResultError implements IJsonData {
    private final String status = "error";
    private final String data;
    
    public ResultError(String errorText) {
        this.data = errorText;
    }

    @Override
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
