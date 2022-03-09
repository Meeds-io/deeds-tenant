# Addon: deeds-tenant

An addon that provides packaged Set of Features used for providing Meeds product for DEEDs owners as SaaS

## Configuration options

| VARIABLE               | MANDATORY | DEFAULT VALUE | DESCRIPTION                                                                               |
|------------------------|-----------|---------------|-------------------------------------------------------------------------------------------|
| meeds.tenantManagement.nftId | NO |  | DEED NFT Id corresponding to current installation. When not set, no Tenant Manager will be selected. |
| meeds.blockchain.networkUrl        | NO  |  | Blockchain HTTPs URL using infura or Alchemy by example |
| meeds.blockchain.tenantProvisioningAddress  | NO        |  | Blockchain Address of TenantProvisioningStrategy.sol contract deployed on Mainnet |
| meeds.register.metamask.allowUserRegistration        | YES        | false | Whether Allow Sign Up users using their MEtamask or not |
| meeds.login.metamask.secureRootAccessWithMetamask | YES        | true | Whether secure root user access by using Metamask only or not  |
| meeds.login.metamask.allowedRootAccessWallets | NO        | | The list of wallets, separated by a commar, who will acces the platform as root user when secured with Metamask |
