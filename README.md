Prerequsities
• I'm going to use the Keycloak cloud environment to run an example of the Keycloak server. You can learn how to build a keycloak docker service in the keycloak-in-docker post.
• The Keycloak server will be available under the http://localhost:8024/auth/  url. The admin credentials are keycloak:keycloak.
Roles in Keycloak
Role is an abstraction that helps to incorporate access details and privileges into a convenient concept. Thanks to this, we can easily assign a set of permissions to users, user groups or more complex roles.
Keycloak roles are defined in a dedicated namespace so that all users with the same roles have identical permissions in that namespace. In other words, realm-level roles are a global namespace for a given realm, while client roles are namespaces intended for specific applications.
In summary, we can manage user permissions in Keycloak with:
• Realm roles
• Client roles
• Groups
• Composite roles
Steps to asign roles to users in Keycloak
Creating a Realm
Point to the top of the left pane.

Click Add Realm.

Enter a name for the realm.

Click Create.
Creating an Application with Spring Cloud Gateway
Let’s go to https://start.spring.io  and create an application with the following dependencies.
• Gateway
• OAuth2 Client
Once you generate and download the application, we will create a simple RestController as follows:-
//
@RestController
public class Controller {

@GetMapping("/")
public String index(Principal principal) {
return principal.getName();
}
}
//

Here we are returning the name (Id of the Keycloak user) from the principal Object which is created by spring security once the user logs in.
Now, let's protect this endpoint with a security configuration.

Screenshot (79).png

Here, we set that, any request that comes in must be authenticated, and in case of a not logged-in user, it should use the OAuth2 login page.
Next, We set the properties to register the Oauth2 Keycloak client in our application.

Setting Application Property values
This contains two parts. Setting the provider properties and registering the client information properties.

• Provider’s properties — The provider of the OAuth2 mechanism i.e the realm.
• Client properties — These are the properties of the Keycloak client to communicate with the realm.
Setting Provider Properties
To set the provider, we need the issuer-URI. For this, you need to go back to your realm setting section and under the “General” tab, you have endpoints. Click on the “OpenId Endpoint Configuration” link and you should get a JSON, containing all the required information. E.g, for the realm we just created, here is a small snippet of the output.

You then set the issuer URI for a provider name called “my-keycloak-provider” like the following.
Screenshot (80).png

Note: The provider name can be a custom value, but you need to use this exact name while using it in the client registration properties.

Setting Client Registration Properties
Next, we will set the client registration properties under the registration name “Spring- cloud-gateway”.
Note: The client registration name can be any custom name. It is used to just identify the client in your application.
Here, we set the client Id we created in Keycloak and the client secret from the client’s “credentials” tab in Keycloak. We also set the provider name, from the properties before, and the redirect URI which we had registered while creating the client in the Keycloak. Also, Since we will be using the authorization code grant type for the OAuth2 flow, we set the authorization grant type to “authorization_code”.
With all the configuration done, Let’s start the application.

Starting the application
Since we set gateway application to server.port=9090, the application starts at 9090. When we open http://localhost:9090  on the web browser, It immediately redirects to the login page from Keycloak as we are querying the root resource.
Screenshot (81).png

Understanding the OAuth2 Open ID Connect Flow:
In the diagram below, I have summarised the flow of how the OAuth2 Connect ID flow works. It starts off with the user requesting a resource, then authenticates itself and gets a response once he is identified.

Screenshot (82).png

The OAuth2 flow is up to the request to get the access token. Once you get the access token, the application makes a request to get the user details. This part belongs to OpenId Connect to get the identity of the user.
So with this, we were able to integrate the Spring Cloud Gateway with Keycloak and set up OAuth2 OpenId Connect to authenticate the user.
Next, we will integrate a backend service to this API Gateway as an OAuth2 resource server and check the user for authorization. This will be in the next article I am currently working on.

Screenshot (76).png

Adding a custom keycloak role to the user:
Let’s add a role to the user in order to allow it to access the resource server.
For this, we will go to our realm and under the roles section and create a role called “product_read”.
Once the role is created, we will then assign this role to our “Springuser” user. To do that, go to the “Users” section and then select the user “springuser”. Once you are in the user's settings, go to the “Role Mappings” tab and add the role to the user as follows.

Creating a Resource Server:
Since we already have the code for the gateway application we will use the same and add a resource server to it
To create the resource server, let's go to https://start.spring.io  and create an application called “product-service” with the following dependencies.
• OAuth2 resource server
• Spring Web
Once you generate and download the project, we will create a simple RestController that provides access to product resources.
Screenshot (83).png

Here I am protecting the GET call with the “product_read” role which we had created in Keycloak. This means if the user can access the resource only if it has the role “product_read”.
Next, We will add some properties to application.yaml
Screenshot (84).png

We can get this JWK URI from the “OpenId Connect Configuration” on the realm settings page. This JWK URI is required to validate the JWT token that comes in with the request.
Next, let’s set up the security configuration.
Screenshot (85).png
Here, we have added the@EnableGlobalMethodSecurity annotation, to enable method-level security in our application. We then create a custom authorities converter. This converter will take out the keycloak roles (that are set as claims) from the JWT token and set them as authorities in spring security for role-based access.
Let’s look at the converter code
Screenshot (85).png

In this converter, we extract the “realmaccess” claims and then convert them to roles, using the ROLE as a prefix. Spring security requires this prefix to interpret them as roles.
The JWT payload has two parts, the “realm_access” and the “scope”. By default, the OAuth2 resource server JWT converter uses the “scope” claims. But these claims are part of the client scope i.e the client that was used in the API Gateway. If you go to the “Client Scopes” section in the client’s setting in Keycloak, you would find these scopes.
So we use the converter to extract the realm roles and use them as authorities in our spring application.
With all of this, we are done with creating the resource server.
Now, to connect it to the API Gateway application, we would have to make some changes to the API Gateway. Let’s have a look at that.

Connecting Resource Server to API Gateway
To connect the product service resource server, We will add a route to the properties file of the API Gateway

Screenshot (80).png

Here we are setting a route for any path request matching /product will be directed to the resource server (product-service) that is running at localhost at port 9191.
In the default-filters section, we would have to add “TokenRelay”, so that the API Gateway passes the JWT access token to the resource server.
With these properties, we are set to now run both the applications, i.e the API gateway and the product service.

Running the Applications:

First build the project

mvn clean install
You can start the API Gateway and product service application using the commands.

java -jar target/spring-cloud-gateway-keycloak-oauth2-0.0.1-SNAPSHOT.jar_

java -jar target/product-service-0.0.1-SNAPSHOT.jar_

The API Gateway runs at 9090 and the product service runs at 9191. Now let's go to the browser and call the following URL localhost:9090/product. On accessing the product resource from the API Gateway, we are redirected to the keycloak login page which is running at management cloud .
Screenshot (86).png

Once you log in, you get the response from the resource server containing the User Id from Keycloak.

Screenshot (70).png

Conclusion
In this article, we integrated a Spring Security and Spring Cloud Gateway to our application. We also Integrated Keycloak’s OAuth2 OpenId Connect (OIDC) for authentication in the API Gateway and also performed a role-based authorization control inside the resource server with the JWT token sent from the API Gateway.
