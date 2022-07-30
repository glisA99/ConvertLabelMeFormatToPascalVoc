package main;

import transformations.Transformations;

public class Main {

    public static void main(String[] args) throws Exception {
        String path = "./src/main/resources/sample-json.json";
        Transformations.convertJsonToPascalVoc(path,"png","random_db");
    }

}
