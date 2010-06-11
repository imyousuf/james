/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.testing;

import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLOptionDescriptor;
import org.apache.avalon.excalibur.cli.CLUtil;

/**
 * Process Protocol Simulator Options. This is required input to
 * Protocol Simulator
 */
public class ProtocolSimulatorOptions {
    // ------- options. See constructor ------
    final String host;
    final int port;
    final String template;
    final int iterations;
    final int workers;

    /** 
     * @param host server host to connect to
     * @param port server port to connect to
     * @param template Protocol simulation template file 
     * @param interations test iterations 
     * @param workers number of parallel threads 
     */
    public ProtocolSimulatorOptions(String host, int port, 
                                    String template, 
                                    int iterations,int workers) 
    {
        this.host = host;
        this.port = port;
        this.template = template;
        this.iterations = iterations;
        this.workers = workers;
    }

    // --- process options ----
    private static interface OptionTag {
        char HELP = 'h';
        char HOST = 's';
        char PORT = 'p';
        char TEMPLATE = 't';
        char ITERATIONS = 'i';
        char WORKERS = 'w';
    } // interface OptionTag

    private static final CLOptionDescriptor[] OPTIONS = {
        new CLOptionDescriptor("server",
                               CLOptionDescriptor.ARGUMENT_REQUIRED,
                               OptionTag.HOST,
                               "Remote Server Host To Test Against"),
        new CLOptionDescriptor("port",
                               CLOptionDescriptor.ARGUMENT_REQUIRED,
                               OptionTag.PORT,
                               "Remote Server Port To Test Against"),
        new CLOptionDescriptor("template",
                               CLOptionDescriptor.ARGUMENT_REQUIRED,
                               OptionTag.TEMPLATE,
                               "Protocol Session Template"),
        new CLOptionDescriptor("workers",
                               CLOptionDescriptor.ARGUMENT_REQUIRED,
                               OptionTag.WORKERS,
                               "Number Of Concurrent Simulations. Default Is 1."),
        new CLOptionDescriptor("iterations",
                               CLOptionDescriptor.ARGUMENT_REQUIRED,
                               OptionTag.ITERATIONS,
                               "Number Of Protocol Simulations Iterations Per Worker. Default Is 1."),
        new CLOptionDescriptor("help",
                               CLOptionDescriptor.ARGUMENT_OPTIONAL,
                               OptionTag.HELP,
                               "Usage Help"),
    };
    

    /** 
     * parse command line options 
     * @return Options or null if there has been parsing error 
     */
    static ProtocolSimulatorOptions getOptions(String[] args) {
        // parse options.
        CLArgsParser parser = new CLArgsParser( args, OPTIONS );
        if( parser.getErrorString() != null ) {
            System.out.println("Error: " + parser.getErrorString());
            System.out.println("try --help option");
            return null;
        }

        // set defaults
        boolean help = false;
        String host = null;
        int port = -1;
        String template = null;
        int iterations = 1;
        int workers = 1;

        // collect options
        CLOption[] option = (CLOption[])parser.getArguments()
            .toArray(new CLOption[0]);
        for( int i = 0; i < option.length; i++ ) {
            CLOption opt = option[i];
            switch(opt.getId()) {
            case OptionTag.HELP:
                help = true;
                break;
            case OptionTag.HOST:
                host = opt.getArgument();
                break;
            case OptionTag.PORT:
                port = Integer.parseInt(opt.getArgument());
                break;
            case OptionTag.TEMPLATE:
                template = opt.getArgument();
                break;
            case OptionTag.ITERATIONS:
                iterations = Integer.parseInt(opt.getArgument());
                break;
            case OptionTag.WORKERS:
                workers = Integer.parseInt(opt.getArgument());
                break;
            default:
                help = true;
                break;
            }
        }
        if ( host == null || port == -1 || template == null || help ) {
            System.out.println(CLUtil.describeOptions(OPTIONS));
            return null;
        }
        return new ProtocolSimulatorOptions(host,port,template,iterations,workers);
    }
}
