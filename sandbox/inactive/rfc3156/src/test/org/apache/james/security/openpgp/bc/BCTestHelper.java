/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.security.openpgp.bc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

public class BCTestHelper {
    
    public static final void registerDataHandlers() throws Exception {
          MailcapCommandMap commandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
          commandMap.addMailcap("application/pgp-signature;; org.apache.james.util.mail.handlers.pgp_signature");
    }
    
    public static final PGPSecretKey loadStandardPGPSecretKey() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        InputStream in = PGPUtil.getDecoderStream(new BufferedInputStream(
                new FileInputStream("src/test/org/apache/james/security/openpgp/bc/secring.gpg")));
        PGPSecretKeyRingCollection rings = new PGPSecretKeyRingCollection(in);
        PGPSecretKey result = rings.getSecretKey(8203466815430217482l);
        return result;
    }
    
    public static final PGPPrivateKey loadStandardPGPPrivateKey() throws Exception {
        PGPSecretKey secretKey = loadStandardPGPSecretKey();
        PGPPrivateKey result = secretKey.extractPrivateKey(password(), "BC");
        return result;
    }

    public static char[] password() {
        return "password".toCharArray();
    }
    
    public static final PGPSignature createSignature(String fileName) throws Exception {
        PGPSecretKey secretKey = loadStandardPGPSecretKey();
        PGPPrivateKey privateKey = secretKey.extractPrivateKey(password(), "BC");
        PGPSignatureGenerator generator = new PGPSignatureGenerator(secretKey.getPublicKey().getAlgorithm(),
                PGPUtil.SHA1, "BC");
        generator.initSign(PGPSignature.BINARY_DOCUMENT, privateKey);
        File file = new File(fileName);
        FileInputStream in = new FileInputStream(file);
        int nextByte = in.read();
        while(nextByte >= 0) {
            generator.update((byte) nextByte);
            nextByte = in.read();
        }
        PGPSignature result = generator.generate();
        return result;
    }
    

    public static final boolean isValidSignature(String signature, File document) throws IOException, InterruptedException {
        File file = File.createTempFile("james", "asc");
        try
        {
            FileWriter writer = new FileWriter(file);
            writer.write(signature);
            writer.close();
            
            File home = new File("src/test/org/apache/james/security/openpgp/bc");
            
            final String command = "gpg --homedir " 
                + home.getAbsolutePath() + " --verify " + file.getAbsolutePath() + " " + document.getAbsolutePath();
            boolean result = true;
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("This test requires Gnu Privacy Guard. Ignoring test.");
            }
            if (process != null) {
                result = interpretResults(process);
            }
            return result;
        } finally {
            file.delete();
        }
    }

    private static boolean interpretResults(Process process) throws InterruptedException, IOException {
        final int exitValue = process.waitFor();
        final boolean success = exitValue == 0;
        if (!success)
        {
            System.out.println("FAILURE");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = reader.readLine();
            while(line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
        }
        return success;
    }

}
