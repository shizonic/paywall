/*
 *************************************************************************
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
package org.lightningj.paywall.lightninghandler.lnd;

import org.lightningj.lnd.wrapper.ClientSideException;
import org.lightningj.lnd.wrapper.message.Chain;
import org.lightningj.lnd.wrapper.message.GetInfoResponse;
import org.lightningj.paywall.InternalErrorException;
import org.lightningj.paywall.vo.*;
import org.lightningj.paywall.vo.amount.CryptoAmount;
import org.lightningj.paywall.vo.amount.Magnetude;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class with methods for converting between LND wrapper objects and paywall-core objects.
 *
 * Created by philip on 2018-11-25.
 */
public class LNDHelper {

    static Logger log = Logger.getLogger(LNDHelper.class.getName());

    String supportedCurrency;
    Clock clock = Clock.systemDefaultZone();

    private static Map<String,String> CHAIN_VALUE_TO_CURRENCY_CODE = new HashMap<>();
    static{
        CHAIN_VALUE_TO_CURRENCY_CODE.put("bitcoin", CryptoAmount.CURRENCY_CODE_BTC);
        CHAIN_VALUE_TO_CURRENCY_CODE.put("litecoin", CryptoAmount.CURRENCY_CODE_LTC);
    }

    /**
     * Constructor of LND helper methods parsing info from LND node.
     *
     * @param infoResponse the GetInfoResponse from the LND node.
     * @throws InternalErrorException if no supported currency was given.
     */
    public LNDHelper(GetInfoResponse infoResponse) throws InternalErrorException{
        try{
            for(Chain chain : infoResponse.getChains()){
                String currencyCode = CHAIN_VALUE_TO_CURRENCY_CODE.get(chain.getChain());
                if(currencyCode != null){
                    supportedCurrency = currencyCode;
                    break;
                }
            }
        }catch (ClientSideException e){
            throw new InternalErrorException("Error parsing LND crypto currency chain from node info, message: " + e.getMessage());
        }
        if(supportedCurrency == null){
            throw new InternalErrorException("Error in LightningHandler, no supported crypto currency could be found in node info.");
        }
    }

    /**
     * Default constructor when supportedCurrency comes from configuration
     *
     * @param supportedCurrency the configured currency code.
     */
    public LNDHelper(String supportedCurrency){
        this.supportedCurrency = supportedCurrency;
    }

    /**
     * Help method to convert a LND Invoice to a Invoice value object.
     * @param nodeInfo the related node info from LND node.
     * @param lndInvoice the LND invoice to convert.
     * @return the converted Invoice.
     */
    public Invoice convert(NodeInfo nodeInfo, org.lightningj.lnd.wrapper.message.Invoice lndInvoice) {
        Invoice invoice = new Invoice();
        invoice.setBolt11Invoice(lndInvoice.getPaymentRequest());
        invoice.setExpireDate(Instant.ofEpochSecond(lndInvoice.getCreationDate() + lndInvoice.getExpiry()));
        invoice.setInvoiceDate(Instant.ofEpochSecond(lndInvoice.getCreationDate()));
        invoice.setPreImageHash(lndInvoice.getRHash());
        invoice.setDescription(lndInvoice.getMemo());
        invoice.setNodeInfo(nodeInfo);
        invoice.setSettled(lndInvoice.getSettled());
        if(lndInvoice.getSettled()){
            invoice.setSettlementDate(Instant.ofEpochSecond(lndInvoice.getSettleDate()));
        }
        invoice.setInvoiceAmount(new CryptoAmount(lndInvoice.getValue(), supportedCurrency));
        if(lndInvoice.getAmtPaidMsat() % 1000 == 0){
            invoice.setSettledAmount(new CryptoAmount(lndInvoice.getAmtPaidSat(), supportedCurrency));
        }else{
            invoice.setSettledAmount(new CryptoAmount(lndInvoice.getAmtPaidMsat(), supportedCurrency, Magnetude.MILLI));
        }

        return invoice;
    }

    /**
     * Method to generate an LND Invoice from a PreImageData and ConvertedOrder
     * @param preImageData the preImageData value object to the invoice preimage and hash from.
     * @param paymentData the payment data to generate invoice value and expire date from.
     * @return a newly generate LND Invoice object that can be used with lnd api to add.
     * @throws InternalErrorException if specified crypto amount is unsupported by LND implementation.
     */
    public org.lightningj.lnd.wrapper.message.Invoice genLNDInvoice(PreImageData preImageData, ConvertedOrder paymentData) throws InternalErrorException{
        checkCryptoAmount(paymentData.getConvertedAmount());

        org.lightningj.lnd.wrapper.message.Invoice retval = new org.lightningj.lnd.wrapper.message.Invoice();
        retval.setRPreimage(preImageData.getPreImage());
        retval.setRHash(preImageData.getPreImageHash());
        // Only Magnitude NONE is supported, so no check is necessary.
        retval.setValue(paymentData.getConvertedAmount().getValue());
        if(paymentData.getDescription() != null) {
            retval.setMemo(paymentData.getDescription());
        }
        // Expire time is number of seconds the invoice should be valid.
        retval.setExpiry(paymentData.getExpireDate().getEpochSecond() - clock.instant().getEpochSecond());

        return retval;
    }

    /**
     * Method that the specified crypto amount is supported by LND.
     * @param amount the crypto amount to used in LND invoice
     * @throws InternalErrorException if crypto amount contained invalid values.
     */
    protected void checkCryptoAmount(CryptoAmount amount) throws InternalErrorException{
        if(amount.getMagnetude() != Magnetude.NONE){
            throw new InternalErrorException("Error in LightningHandler, Invalid crypto currency amount in payload data. Unsupported magnetude: " + amount.getMagnetude());
        }
        if(!supportedCurrency.equals(amount.getCurrencyCode())){
            throw new InternalErrorException("Error in LightningHandler, Unsupported crypto currency code in payload data: " + amount.getCurrencyCode());
        }
    }

    /**
     * Method to parse a GetInfoResponse into a NodeInfo value object.
     * @param infoResponse the GetInfoResponse from LND node.
     * @return a parsed NodeInfo value object.
     * @throws InternalErrorException if problem occurred parsing the LND node data.
     */
    public NodeInfo parseNodeInfo(GetInfoResponse infoResponse) throws InternalErrorException{
        try {
            NodeInfo retval = new NodeInfo();
            List<String> uris = infoResponse.getUris();
            if (uris.size() > 0) {
                if (uris.size() > 1) {
                    log.log(Level.INFO, "LND Node Info contains " + uris.size() + " URIs, using the first one: " + uris.get(0));
                }
                retval.setConnectString(uris.get(0));
            }
            retval.setNodeNetwork(infoResponse.getTestnet() ? NodeInfo.NodeNetwork.TEST_NET : NodeInfo.NodeNetwork.MAIN_NET);

            return retval;
        }catch(ClientSideException e){
            throw new InternalErrorException("Internal Error parsing LND Node info: " + e.getMessage(),e);
        }
    }

}
