package com.amrut.prabhu.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import java.security.Principal;

@RestController
public class Controller {

    @GetMapping("/product")
    @RolesAllowed({"product_read"})
    public String getProduct(Principal principal) {
        return "Response from Product Service, User Id:" + principal.getName();
        @GetMapping("/user")
        @RolesAllowed({"user"})
        public String getUser(Principal principal) {
            return " " + principal.getName();
            @GetMapping("/test")
            @RolesAllowed({"test2_role"})
            public String getTest(Principal principal) {
                return "Response from Test Service, User Id:" + principal.getName();

    }
}
