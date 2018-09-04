package advnetsys.adbc;

class Spaces {

  String spaces = new String("                                                                                                                      ");

  public String createSpaces( int numSpacesParm ) {
    return spaces.substring( 0, numSpacesParm );
  }

}
