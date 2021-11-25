
package com.nixwin;

import com.jaunt.*;
import com.jaunt.component.Form;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws SearchException, ResponseException, IOException {
        UserAgent userAgent = new UserAgent();

//        userAgent.visit("https://www.notonthehighstreet.com/admin/session/new");
//        Form form = userAgent.doc.getForm(0);
//        form.setTextField("email", "info@.com");
////        form.setTextField("shortcode", "");
//        form.setPassword("password", "");
//
//        try {
//            form.submit();
//        } catch (ResponseException e) {
//            e.printStackTrace();
      //  }
  //      userAgent.visit("https://embers.notonthehighstreet.com/admin#products:630627");
    //    System.out.println(userAgent.doc.toString());


        Writer writer = new FileWriter("Embers.csv");
        userAgent.visit("https://preview.notonthehighstreet.com/partners/embers/products?view=many");
        Elements productCells = userAgent.doc.findEvery("<div class='product_grid_cell_module.*'>");
        productCells.toList().stream().
                map(cell -> {
                    Product product = new Product();
                    try {
                        product.name = cell.getAt("data-product-name");
                        product.productId = cell.getAt("data-product-id");
                        product.productUrl = cell.findFirst("a").getAt("href");
                    } catch (NotFound notFound) {
                        return null;
                    }
                    return product;
                })
                .map(product -> fillInImage(userAgent, product))
                .map(product -> fillInPrice(userAgent, product))
                .map(product -> fillInDescription(userAgent, product))
                .forEach(p -> {
                    try {
                        writer.write(p.toString());
                        writer.flush();
                        System.out.println(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

    }

    private static Product fillInDescription(UserAgent userAgent, Product product) {
        String description = "";
        List<Element> paragraphs = null;
        try {
            paragraphs = userAgent.doc.findFirst("<div class='content'>").findEvery("p").toList();

            for(Element paragraph: paragraphs){
                if(paragraph.getText().startsWith("Embers is a London based")) {
                    break;
                } else {
                    if(!description.isEmpty()) {
                        description = description + "\n\n";
                    }
                    description = description + paragraph.getText();
                }
            }
            product.description = description;
            return product;
        } catch (NotFound notFound) {
            notFound.printStackTrace();
        }
       return null;
    }

    private static Product fillInPrice(UserAgent userAgent, Product product) {
        try {
            String price = userAgent.doc.findFirst("<span class='currency_GBP'>").getAt("data-current-price");
            product.price = price;
        } catch (NotFound notFound) {
            notFound.printStackTrace();
        }
        return product;
    }

    private static Product fillInImage(UserAgent userAgent, Product product) {
        try {
            userAgent.visit(product.productUrl);
            List<String> stringStream = userAgent.doc.findFirst("<div class='browsable'>").findEvery("a").toList().stream().map(item -> {
                try {
                    return "http:" + item.getAt("href_normal");
                } catch (NotFound notFound) {
                    notFound.printStackTrace();
                    return "";
                }
            }).collect(Collectors.toList());
            product.imageUrls.addAll(stringStream);
        } catch (ResponseException e) {
            e.printStackTrace();
        } catch (NotFound notFound) {
            notFound.printStackTrace();
        }
        return product;
    }

    private static class Product {
        public String productId;
        public String name;
        public String productUrl;
        public String sku;
        public String price;
        public String description;
        public List<String> imageUrls = new ArrayList<>();


        public String toString() {

            return String.join(" ,", StringEscapeUtils.escapeCsv(name), productId,  price) + " ," + escape(description) + "," + String.join(" ,", imageUrls);
        }

        private String escape(String description) {
            return StringEscapeUtils.escapeCsv(description);
        }

    }
}
