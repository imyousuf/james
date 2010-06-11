Java Mail Servlet API Extentions
--------------------------------

Written and designed by Stefano Mazzocchi and Pierpaolo Fumagalli.

As it can be seen by the API structure (see the /javadoc directory 
and the /src directory for souce code), the mail servlet design 
fits nicely on top of the existing servlet architecture, proving the 
need for servlet protocol abstraction (that was recently under discussion).

The mail servlets were designed on top of the javax.servlet.* package alone,
meaning that no other package (but the core Java APIs) are needed to compile
and execute mail servlets.

JavaMail extentions were not used to simply the learning process
and reduce servlet development overhead to a minimum. (we believe 
JavaMail to be way to general and client oriented to find a place
in this context). Note that this doesn't count for mail servlet engine
implementation that could use JavaMail to implement the features specified
by the mail servlet APIs.

The mail servlet examples proposed (see the the /servlets directory) show
how complex mail tasks may be handled with just a few line of code. It doesn't
need to be underlined the power this architecture would bring on existing
mail handling systems such as Sendmail or on completely new approaches like
Apache James.

The authors agree to donate this package and the ideas contained to 
Sun Microsystems if:
1) credits are maintained throught the code and documentation/specification
2) these extentions are incorporated into future releases of the the Servlet 
API Architecture
3) the classes remain free and open sourced.

                                                     October 27, 1998

                                                     Stefano Mazzocchi
                                                    Pierpaolo Fumagalli