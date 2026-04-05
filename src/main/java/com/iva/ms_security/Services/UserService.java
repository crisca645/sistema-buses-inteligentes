package com.iva.ms_security.Services;

import com.iva.ms_security.Models.Profile;
import com.iva.ms_security.Models.RegisterRequest;
import com.iva.ms_security.Models.Session;
import com.iva.ms_security.Models.User;
import com.iva.ms_security.Repositories.ProfileRepository;
import com.iva.ms_security.Repositories.SessionRepository;
import com.iva.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    public List<User> find(){

        return this.theUserRepository.findAll();
    }

    public User findById(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        return theUser;
    }

  /**  public User create(User newUser){
        newUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
        return this.theUserRepository.save(newUser);}
   * esta parte de codigo ya no va, se modifico para los siguientes cambios
   */
  public User create(RegisterRequest request) {
      if (request == null) {
          throw new RuntimeException("Solicitud inválida");
      }

      String name = request.getName() != null ? request.getName().trim() : null;
      String lastname = request.getLastname() != null ? request.getLastname().trim() : null;
      String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
      String password = request.getPassword();
      String confirmPassword = request.getConfirmPassword();

      if (name == null || name.isEmpty()) {
          throw new RuntimeException("El nombre es obligatorio");
      }
      if (lastname == null || lastname.isEmpty()) {
          throw new RuntimeException("El apellido es obligatorio");
      }
      if (email == null || email.isEmpty()) {
          throw new RuntimeException("El email es obligatorio");
      }
      if (!isValidEmail(email)) {
          throw new RuntimeException("El email no tiene un formato válido");
      }

      User existingUser = this.theUserRepository.getUserByEmail(email);
      if (existingUser != null) {
          throw new RuntimeException("El email ya está registrado");
      }

      if (password == null || password.isEmpty()) {
          throw new RuntimeException("La contraseña es obligatoria");
      }
      if (!isValidPassword(password)) {
          throw new RuntimeException("La contraseña no cumple los requisitos de seguridad");
      }
      if (confirmPassword == null || !password.equals(confirmPassword)) {
          throw new RuntimeException("Las contraseñas no coinciden");
      }

      User newUser = new User();
      newUser.setName(name);
      newUser.setLastname(lastname);
      newUser.setEmail(email);
      newUser.setPassword(theEncryptionService.convertSHA256(password));

      User savedUser = this.theUserRepository.save(newUser);

      try {
          RestTemplate restTemplate = new RestTemplate();
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);

          Map<String, String> emailBody = new HashMap<>();
          emailBody.put("to", savedUser.getEmail());
          emailBody.put("subject", "Cuenta creada exitosamente");
          emailBody.put("body",
                  "<h1>Bienvenido, " + savedUser.getName() + "!</h1>" +
                          "<p>Tu cuenta en el sistema de buses ha sido creada exitosamente.</p>" +
                          "<p>Ya puedes iniciar sesión con tu email y contraseña.</p>"
          );

          HttpEntity<Map<String, String>> emailRequest = new HttpEntity<>(emailBody, headers);
          restTemplate.postForEntity("http://localhost:5000/send-email", emailRequest, String.class);
      } catch (Exception e) {
          System.out.println("Error enviando email de confirmación: " + e.getMessage());
      }
      return savedUser;
  }



    public User update(String id, User newUser){
        User actualUser=this.theUserRepository.findById(id).orElse(null);

        if(actualUser!=null){
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            actualUser.setLastname(newUser.getLastname());
            actualUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
            this.theUserRepository.save(actualUser);
            return actualUser;
        }else{
            return null;
        }
    }

    public void delete(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        if (theUser!=null){
            this.theUserRepository.delete(theUser);
        }
    }

    /**
     * Permite asociar un usuario y un perfil . Para que funcione ambos
     * ya deben estar creados.
     *
     * @param userId
     * @param profileId
     * @return
     */
    public boolean addProfile(String userId, String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile= theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(theUser);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }
    }

    public boolean removeProfile(String userId, String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile=this.theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(null);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }

    }


    /**
     * Permite asociar un usuario y una sesión. Para que funcione ambos
     * ya deben de existir en la base de datos
     * @param userId
     * @param sessionId
     * @return
     */
    public boolean addSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(theUser);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }
    public boolean removeSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(null);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }
    //forma de correo electrónico.
    private boolean isValidEmail(String email){
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }
    //Este metodo valida que la contraseña sea segura
    private boolean isValidPassword(String password){
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$";
        return Pattern.matches(passwordRegex, password);
    }

}

