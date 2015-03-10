<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <title>Test Bootstrap</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="">
        <meta name="author" content="">

        <link href="bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
        <link href="bootstrap/css/bootstrap-responsive.css" rel="stylesheet" />
    </head>

    <body>
        
          <h3>Within a navbar</h3>
          
       
              <div class="navbar-inner">
                <div class="container" style="width: auto;">
                  <a class="brand" href="#">Project Name</a>
                  
                  <ul class="nav" role="navigation">
                    <li class="dropdown">
                      <a id="drop1" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown">
                          Dropdown <b class="caret"></b>
                      </a>
                      <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                        <li><a tabindex="-1" href="http://google.com">Go to google</a></li>
                      </ul>
                    </li>
                   
                  </ul>
                  
                </div>
              </div>
          
         
          <table>
              <tbody>
                  <tr>
                      <td>
                          <div class="dnpopup">
                              <a href="#" id="dn1"
                                 class="btn btn-small btn-danger" 
                                 rel="popover" title="A Title1" 
                                 data-content="cert dn1 will be here">DN1</a>
                          </div>
                      </td>
                  </tr>
                  <tr>
                      <td>
                          <div class="dnpopup">
                              <a href="#" id="dn2"
                                 class="btn btn-small btn-danger" 
                                 rel="popover" title="A Title2" 
                                 data-content="cert dn2 will be here">DN2</a>
                          </div>
                      </td>
                  </tr>
                  <tr>
                      <td>
                          <div class="dnpopup">
                              <a href="#" id="dn3"
                                 class="btn btn-small btn-danger" 
                                 rel="popover" title="A Title3" 
                                 data-content="cert dn3 will be here">DN3</a>
                          </div>
                      </td>
                  </tr>
              </tbody>
          </table>


        <%--<div class="navbar navbar-inverse navbar-fixed-top">
          <div class="navbar-inner">
            <div class="container">
              <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </a>
              <a class="brand" href="#">Project name</a>
              <div class="nav-collapse collapse">
                <ul class="nav">
                  <li class="active"><a href="#">Home</a></li>
                  <li><a href="#about">About</a></li>
                  <li><a href="#contact">Contact</a></li>
                  
                  <li class="dropdown">
                      <a class="dropdown-toggle" data-toggle="dropdown" data-target="#" href="/caportal/raop/viewcert">
                          Dropdowns 
                          <b class="caret"></b>
                      </a>
                    <ul class="dropdown-menu">
                      <li><a href="#">Action</a></li>
                      <li><a href="#">Another action</a></li>
                      <li><a href="#">Something else here</a></li>
                      <li class="divider"></li>
                      <li class="nav-header">Nav header</li>
                      <li><a href="#">Separated link</a></li>
                      <li><a href="#">One more separated link</a></li>
                    </ul>
                  </li>
                </ul>

          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>--%>


        <%--
            <div class="container">

      <!-- Main hero unit for a primary marketing message or call to action -->
      <div class="hero-unit">
        <h1>Hello, world!</h1>
        <p>This is a template for a simple marketing or informational website. It includes a large callout called the hero unit and three supporting pieces of content. Use it as a starting point to create something more unique.</p>
        <p><a class="btn btn-primary btn-large">Learn more &raquo;</a></p>
      </div>

      <!-- Example row of columns -->
      <div class="row">
        <div class="span4">
          <h2>Heading</h2>
          <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
          <p><a class="btn" href="#">View details &raquo;</a></p>
        </div>
        <div class="span4">
          <h2>Heading</h2>
          <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
          <p><a class="btn" href="#">View details &raquo;</a></p>
       </div>
        <div class="span4">
          <h2>Heading</h2>
          <p>Donec sed odio dui. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Vestibulum id ligula porta felis euismod semper. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>
          <p><a class="btn" href="#">View details &raquo;</a></p>
        </div>
      </div>

      <hr>

      <footer>
        <p>&copy; Company 2012</p>
      </footer>

    </div> <!-- /container -->
        --%>

        <!-- Le javascript
        ================================================== -->
        <!-- Placed at the end of the document so the pages load faster -->
        <script src="jquery/jquery-1.8.3.min.js"></script>
        <script src="bootstrap/js/bootstrap.min.js"></script>
        
        <script>
            //$("#dn1").popover({offset: 10});
            $(".dnpopup a").closest('a').popover({offset: 10});
            
            //$("tr input[type=button]").popover({offset: 10});
 //          $("a").closest().popover({offset: 10});
           
 //          $('input[type=button]' ).click(function() {
 //  var bid = this.id; // button ID 
 //  var trid = $(this).closest('tr').attr('id'); // table row ID 
 //});
 
 //$('tr input[type=button]').click(function(){
 //   id = $(this).closest('tr').attr('id');
 //});
        </script>

    </body>
</html>
