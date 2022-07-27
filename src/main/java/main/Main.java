package main;

import transformations.Transformations;

public class Main {

    public static void main(String[] args) throws Exception {
        Transformations.convertJsonToPascalVoc("./src/main/resources/sample-json.json", "");

    }

}
