/*
 * ***********************************************************************
 *                                                                       *
 *  LightningJ                                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public License   *
 *  (LGPL-3.0-or-later)                                                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 3 of the License, or any later version.                      *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.lightningj.paywall.spring.response;

import org.lightningj.paywall.paymentflow.InvoiceResult;
import org.lightningj.paywall.requestpolicy.RequestPolicyType;
import org.lightningj.paywall.util.Base64Utils;
import org.lightningj.paywall.vo.Invoice;

import javax.xml.bind.annotation.*;
import java.util.Date;

/**
 * Value object used to return the value of a generated invoice generated by the
 * PaywallInterceptor.
 *
 * @author philip 2019-04-15
 */
@XmlRootElement(name = "InvoiceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InvoiceResponseType", propOrder = {
        "preImageHash",
        "bolt11Invoice",
        "description",
        "invoiceAmount",
        "nodeInfo",
        "token",
        "invoiceDate",
        "invoiceExpireDate",
        "payPerRequest",
        "requestPolicyType",
        "checkSettlementLink",
        "qrLink"
})
public class InvoiceResponse {

    public static final String TYPE = "invoice";

    @XmlTransient
    private String type=TYPE;

    @XmlElement(required = true)
    private byte[] preImageHash;

    @XmlElement(required = true)
    private String bolt11Invoice;

    @XmlElement()
    private String description;

    @XmlElement(required = true)
    private CryptoAmount invoiceAmount;

    @XmlElement()
    private NodeInfo nodeInfo;

    @XmlElement(required = true)
    private String token;

    @XmlElement(required = true)
    private Date invoiceDate;

    @XmlElement(required = true)
    private Date invoiceExpireDate;

    @XmlElement(defaultValue = "false")
    private Boolean payPerRequest;

    @XmlElement(required = true)
    private String requestPolicyType;

    @XmlElement()
    private String checkSettlementLink;

    @XmlElement()
    private String qrLink;

    /**
     * Empty constructor.
     */
    public InvoiceResponse(){}

    /**
     * Default constructor used by PaywallInterceptor.
     *
     * @param invoiceResult The related invoice result
     * @param isPayPerRequest if invoice is required per request or if settlement with be valid for multiple requests
     *                        during a period of time.
     * @param includeNodeInfo if lightning connection information info should be included in the response.
     * @param checkSettlementLink link to check settlement controller.
     * @param qrLink link to generate QR Code controller.
     */
    public InvoiceResponse(InvoiceResult invoiceResult,
                           boolean isPayPerRequest,
                           RequestPolicyType requestPolicyType,
                           boolean includeNodeInfo,
                           String checkSettlementLink,
                           String qrLink){
        Invoice invoice = invoiceResult.getInvoice();
        preImageHash = invoice.getPreImageHash();
        bolt11Invoice = invoice.getBolt11Invoice();
        description = invoice.getDescription();
        invoiceAmount = new CryptoAmount(invoice.getInvoiceAmount());
        if(includeNodeInfo){
            nodeInfo = new NodeInfo(invoice.getNodeInfo());
        }
        token = invoiceResult.getToken();
        invoiceDate = new Date(invoice.getInvoiceDate().toEpochMilli());
        invoiceExpireDate = new Date(invoice.getExpireDate().toEpochMilli());
        this.requestPolicyType = requestPolicyType.name();
        payPerRequest = isPayPerRequest;
        this.checkSettlementLink = checkSettlementLink;
        this.qrLink = qrLink;
    }

    /**
     *
     * @return the type 'invoice', used in JSON conversions only.
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type the type 'invoice', used in JSON conversions only.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return the generated preImageHash from PreImageData which acts as an unique id for the payment.
     */
    public byte[] getPreImageHash() {
        return preImageHash;
    }

    /**
     *
     * @param preImageHash the generated preImageHash from PreImageData which acts as an unique id for the payment.
     */
    public void setPreImageHash(byte[] preImageHash) {
        this.preImageHash = preImageHash;
    }

    /**
     *
     * @return the bolt11 invoice to display for the requester.
     */
    public String getBolt11Invoice() {
        return bolt11Invoice;
    }

    /**
     *
     * @param bolt11Invoice the bolt11 invoice to display for the requester.
     */
    public void setBolt11Invoice(String bolt11Invoice) {
        this.bolt11Invoice = bolt11Invoice;
    }

    /**
     *
     * @return description to display in the invoice. (Optional).
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description description to display in the invoice. (Optional).
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return the amount in the invoice. (Optional)
     */
    public CryptoAmount getInvoiceAmount() {
        return invoiceAmount;
    }

    /**
     *
     * @param invoiceAmount the amount in the invoice. (Optional)
     */
    public void setInvoiceAmount(CryptoAmount invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    /**
     *
     * @return information about the related lightning node. (Optional)
     */
    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     *
     * @param nodeInfo information about the related lightning node. (Optional)
     */
    public void setNodeInfo(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    /**
     *
     * @return the generated JWT invoice token used to track the payment when checking settlement.
     */
    public String getToken() {
        return token;
    }

    /**
     *
     * @param token the generated JWT invoice token used to track the payment when checking settlement.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     *
     * @return the time this invoice was created.
     */
    public Date getInvoiceDate() {
        return invoiceDate;
    }

    /**
     *
     * @param invoiceDate the time this invoice was created.
     */
    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    /**
     *
     * @return the time the invoice will expire.
     */
    public Date getInvoiceExpireDate() {
        return invoiceExpireDate;
    }

    /**
     *
     * @param invoiceExpireDate the time the invoice will expire.
     */
    public void setInvoiceExpireDate(Date invoiceExpireDate) {
        this.invoiceExpireDate = invoiceExpireDate;
    }

    /**
     *
     * @return if payment is for this api is for one time only or usage is for a given period of time.
     */
    public Boolean getPayPerRequest() {
        return payPerRequest;
    }

    /**
     *
     * @param payPerRequest if payment is for this api is for one time only or usage is for a given period of time.
     */
    public void setPayPerRequest(Boolean payPerRequest) {
        this.payPerRequest = payPerRequest;
    }

    /**
     *
     * @return specifying type of policy used for aggregating significant request data.
     */
    public String getRequestPolicyType() {
        return requestPolicyType;
    }

    /**
     *
     * @param requestPolicyType specifying type of policy used for aggregating
     * significant request data.
     */
    public void setRequestPolicyType(String requestPolicyType) {
        this.requestPolicyType = requestPolicyType;
    }

    /**
     *
     * @return link to settlement controller for checking payment state.
     */
    public String getCheckSettlementLink() {
        return checkSettlementLink;
    }

    /**
     *
     * @param checkSettlementLink link to settlement controller for checking payment state.
     */
    public void setCheckSettlementLink(String checkSettlementLink) {
        this.checkSettlementLink = checkSettlementLink;
    }

    /**
     *
     * @return link to QR Code generator controller.
     */
    public String getQrLink() {
        return qrLink;
    }

    /**
     *
     * @param qrLink link to QR Code generator controller.
     */
    public void setQrLink(String qrLink) {
        this.qrLink = qrLink;
    }

    @Override
    public String toString() {
        return "InvoiceResponse{" +
                ", preImageHash='" + (preImageHash != null ? Base64Utils.encodeBase64String(preImageHash) : null)  + '\'' +
                ", bolt11Invoice='" + bolt11Invoice + '\'' +
                ", description='" + description + '\'' +
                ", invoiceAmount=" + invoiceAmount +
                ", nodeInfo=" + nodeInfo +
                ", token='" + token + '\'' +
                ", invoiceDate=" + invoiceDate +
                ", invoiceExpireDate=" + invoiceExpireDate +
                ", payPerRequest=" + payPerRequest +
                ", requestPolicyType=" + requestPolicyType +
                ", checkSettlementLink='" + checkSettlementLink + '\'' +
                ", qrLink='" + qrLink + '\'' +
                '}';
    }
}