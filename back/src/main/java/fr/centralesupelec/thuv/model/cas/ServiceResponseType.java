//
// Ce fichier a xe9txe9 gxe9nxe9rxe9 par l'implxe9mentation de rxe9fxe9rence
//  JavaTM Architecture for XML Binding (JAXB), v2.3.1-b171012.0423
// Voir <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a>
// Toute modification apportxe9e xe0 ce fichier sera perdue lors de la recompilation du schxe9ma source.
// Gxe9nxe9rxe9 le : 2018.10.14 xe0 05:55:23 PM CEST
//


package fr.centralesupelec.thuv.model.cas;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Classe Java pour serviceResponseType complex type.
 *
 * <p>Le fragment de schxE9ma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="serviceResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="authenticationSuccess" type="{http://www.yale.edu/tp/cas}authenticationSuccessType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serviceResponseType", propOrder = {
    "authenticationSuccess"
})
public class ServiceResponseType {

    @XmlElement(required = true)
    protected AuthenticationSuccessType authenticationSuccess;

    /**
     * Obtient la valeur de la proprixE9txE9 authenticationSuccess.
     *
     * @return
     *     possible object is
     *     {@link AuthenticationSuccessType }
     *
     */
    public AuthenticationSuccessType getAuthenticationSuccess() {
        return authenticationSuccess;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 authenticationSuccess.
     *
     * @param value
     *     allowed object is
     *     {@link AuthenticationSuccessType }
     *
     */
    public void setAuthenticationSuccess(AuthenticationSuccessType value) {
        this.authenticationSuccess = value;
    }

}
