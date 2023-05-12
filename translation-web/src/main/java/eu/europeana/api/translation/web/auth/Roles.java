package eu.europeana.api.translation.web.auth;

import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.web.model.vocabulary.Operations;

/** Mapping between user role and operations Reproduced from Sets API */
public enum Roles implements Role {
  ANONYMOUS(new String[] {Operations.RETRIEVE}),
  EDITOR(
      new String[] {Operations.RETRIEVE, Operations.CREATE, Operations.DELETE, Operations.UPDATE}),
  ADMIN(
      new String[] {
        Operations.RETRIEVE,
        Operations.CREATE,
        Operations.DELETE,
        Operations.UPDATE,
        Operations.ADMIN_ALL
      });

  String[] operations;

  Roles(String[] operations) {
    this.operations = operations;
  }

  public String[] getOperations() {
    return operations;
  }

  @Override
  public String[] getPermissions() {
    return getOperations();
  }

  @Override
  public String getName() {
    return this.name();
  }

  /**
   * This method returns the api specific Role for the given role name
   *
   * @param name the name of user role
   * @return the user role
   */
  public static Role getRoleByName(String name) {
    Role userRole = null;
    for (Roles role : Roles.values()) {
      if (role.name().equalsIgnoreCase(name)) {
        userRole = role;
        break;
      }
    }
    return userRole;
  }
}
