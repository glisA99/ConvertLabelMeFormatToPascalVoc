package main;

import transformations.Transformations;

public class Main {

    public static void main(String[] args) throws Exception {
        String path = "./src/main/resources/sample-json.json";
        String directory_path = "./src/main/resources";
//        Transformations.convertJsonToPascalVoc(path,"png","random_db");
        Transformations.convertAll(directory_path,"png","random_db");
    }

}
