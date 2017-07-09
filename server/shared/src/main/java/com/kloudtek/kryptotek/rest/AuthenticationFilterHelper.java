package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.CryptoEngine;
import com.kloudtek.kryptotek.CryptoUtils;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import com.kloudtek.util.BackendAccessException;
import com.kloudtek.util.InvalidBackendDataException;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.TimeUtils;
import com.kloudtek.util.io.BoundedOutputStream;
import com.kloudtek.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.Date;

import static com.kloudtek.kryptotek.CryptoUtils.fingerprint;
import static com.kloudtek.kryptotek.DigestAlgorithm.SHA256;
import static com.kloudtek.kryptotek.rest.AuthenticationFailedException.Reason.INVALID_SIGNATURE;
import static com.kloudtek.kryptotek.rest.AuthenticationFailedException.Reason.USER_NOT_FOUND;
import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;

/**
 * Created by yannick on 6/25/17.
 */
public abstract class AuthenticationFilterHelper<P, Q> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilterHelper.class);
    public static final long DEFAULT_EXPIRY = 300000L;
    protected Long contentMaxSize;
    protected DigestAlgorithm digestAlgorithm;
    protected long expiry = DEFAULT_EXPIRY;
    protected ReplayAttackValidator replayAttackValidator;
    protected CryptoEngine cryptoEngine;

    public AuthenticationFilterHelper() {
        this(CryptoUtils.getEngine());
    }

    public AuthenticationFilterHelper(CryptoEngine cryptoEngine) {
        this(cryptoEngine, new ReplayAttackValidatorNoOpImpl());
    }

    public AuthenticationFilterHelper(CryptoEngine cryptoEngine, ReplayAttackValidator replayAttackValidator) {
        this(cryptoEngine, null, SHA256, DEFAULT_EXPIRY, replayAttackValidator);
    }

    public AuthenticationFilterHelper(CryptoEngine cryptoEngine, Long contentMaxSize, DigestAlgorithm digestAlgorithm,
                                      long expiry, ReplayAttackValidator replayAttackValidator) {
        this.cryptoEngine = cryptoEngine;
        this.contentMaxSize = contentMaxSize;
        this.digestAlgorithm = digestAlgorithm;
        this.expiry = expiry;
        this.replayAttackValidator = replayAttackValidator;
    }

    public P authenticateRequest(InputStream inputStream, String nonce, String identity, String timestampStr, String signature,
                                 String method, String pathWithoutQuery, String query, Q requestObj) throws AuthenticationFailedException, IOException, InvalidRequestException, InvalidBackendDataException {
        if (nonce == null) {
            throw new InvalidRequestException("header " + HEADER_NONCE + " missing", requestObj);
        }
        if (identity == null) {
            throw new InvalidRequestException("header " + HEADER_IDENTITY + " missing", requestObj);
        }
        if (timestampStr == null) {
            throw new InvalidRequestException("header " + HEADER_TIMESTAMP + " missing", requestObj);
        }
        if (signature == null) {
            throw new InvalidRequestException("header " + HEADER_TIMESTAMP + " missing", requestObj);
        }
        StringBuilder path = new StringBuilder(pathWithoutQuery);
        if (query != null) {
            path.append('?').append(query);
        }
        RESTRequestSigner restRequestSigner = new RESTRequestSigner(method, path.toString(), nonce, timestampStr, identity);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, contentMaxSize != null ? new BoundedOutputStream(content, contentMaxSize, true) : content);
        byte[] contentData = content.toByteArray();
        restRequestSigner.setContent(contentData);
        replaceDataStream(requestObj, new ByteArrayInputStream(contentData));
        try {
            Date timestamp = TimeUtils.parseISOUTCDateTime(timestampStr);
            if (timestamp.after(new Date(System.currentTimeMillis() + expiry))) {
                throw new InvalidRequestException("Unauthorized request (expired timestamp): " + timestampStr, requestObj);
            }
            if (replayAttackValidator.checkNonceReplay(nonce)) {
                throw new InvalidRequestException("Unauthorized request (duplicated nonce): " + nonce, requestObj);
            }
            P principal = findUserPrincipal(identity);
            if (principal == null) {
                throw new AuthenticationFailedException("Unauthorized request (principal not found): " + identity, USER_NOT_FOUND, requestObj);
            }
            if (!verifySignature(identity, principal, restRequestSigner.getDataToSign(), signature)) {
                throw new AuthenticationFailedException("Unauthorized request (invalid signature): " + restRequestSigner.toString(), INVALID_SIGNATURE, requestObj);
            }
            return principal;
        } catch (ParseException e) {
            throw new InvalidRequestException("Invalid timestamp: " + timestampStr, e);
        }
    }

    protected abstract void replaceDataStream(Q requestObj, InputStream inputStream);

    private boolean verifySignature(String identity, P principal, byte[] dataToSign, String signature) throws BackendAccessException, InvalidBackendDataException {
        final byte[] signatureData = StringUtils.base64Decode(signature);
        if (logger.isDebugEnabled()) {
            logger.debug("Verifying REST request - principal: " + principal + " data: " + fingerprint(dataToSign) + " signature: " + fingerprint(signatureData));
        }
        SignatureVerificationKey key = findVerificationKey(principal);
        if (key == null) {
            return false;
        }
        try {
            cryptoEngine.verifySignature(key, digestAlgorithm, dataToSign, signatureData);
            return true;
        } catch (InvalidKeyException e) {
            throw new InvalidBackendDataException("Invalid key for principal " + identity + " found while verifying signature: " + e.getMessage(), e);
        } catch (SignatureException e) {
            return false;
        }
    }

    public String signResponse(P userDetails, byte[] data) throws BackendAccessException, InvalidBackendDataException {
        SigningKey key = findSigningKey(userDetails);
        if (key == null) {
            String msg = "Unable to find key for response signing: ";
            logger.error(msg, (Exception) null);
            throw new InvalidBackendDataException(msg);
        }
        try {
            return StringUtils.base64Encode(cryptoEngine.sign(key, digestAlgorithm, data));
        } catch (InvalidKeyException e) {
            throw new InvalidBackendDataException("Invalid key: " + e.getMessage(), e);
        }
    }

    protected abstract P findUserPrincipal(String identity) throws BackendAccessException;

    protected abstract SignatureVerificationKey findVerificationKey(P principal) throws BackendAccessException;

    protected abstract SigningKey findSigningKey(P principal) throws BackendAccessException;
}
