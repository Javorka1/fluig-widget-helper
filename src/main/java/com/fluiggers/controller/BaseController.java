package com.fluiggers.controller;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluig.sdk.service.SecurityService;
import com.fluig.sdk.service.UserService;

public abstract class BaseController {
    private static final Set<String> ALLOWED_LOGINS = Set.of("gpovoa", "pverginio");

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @EJB(lookup = SecurityService.JNDI_REMOTE_NAME)
    protected SecurityService securityService;

    @EJB(lookup = UserService.JNDI_REMOTE_NAME)
    protected UserService userService;

    @PostConstruct
    private void assertUserAccess() {
        try {
            String login = userService.getCurrent().getLogin();

            if (login != null && ALLOWED_LOGINS.contains(login.toLowerCase())) {
                return;
            }

            Boolean isAdmin = securityService
                .listTenantAdmins(securityService.getCurrentTenantId())
                .stream()
                .anyMatch(user -> user.getLogin().equals(login))
            ;

            if (isAdmin) {
                return;
            }
        } catch (Exception ignore) {
            log.error("Erro não capturado ao validar usuário Administrador", ignore);
        }

        throw new ForbiddenException();
    }
}
