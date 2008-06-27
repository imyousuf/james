/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
            int iterations, int workers) {
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
        CLArgsParser parser = new CLArgsParser(args, OPTIONS);

        if (parser.getErrorString() != null) {
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
        CLOption[] option = (CLOption[]) parser.getArguments().toArray(new CLOption[0]);

        for (int i = 0; i < option.length; i++) {
            CLOption opt = option[i];

            switch (opt.getId()) {
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
        if (host == null || port == -1 || template == null || help) {
            System.out.println(CLUtil.describeOptions(OPTIONS));
            return null;
        }
        return new ProtocolSimulatorOptions(host, port, template, iterations, workers);
    }
}
