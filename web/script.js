var overrideTemp = Number(0.0);

function getNewValues( immediate, initial ) {
  var url = "/heating/all_details";
  if( immediate ) {
    url = url + "/no_wait";
  }
  var qry = $.ajax({
    type: "GET",
    url: url,
    dataType: "json",
    cache: false
  })
    .done( function( data ) {
      processNewValues( data );
    })
    .always( function( data, textStatus, jqXHR ) {
    $( "#goneoutuntil" ).clockTimePicker({
      precision: 10,
      afternoonHoursInOuterCircle: true,
      minimum: moment().add( 1, 'hours' ).format( "HH:mm" ),
      required: true,
      onChange:( function( newValue, oldValue ) {
        var newTime = moment();
        var hours = newValue.substring( 0, 2 );
        var minutes = newValue.substring( 3, 5 );
        newTime.hour(hours);
        newTime.minute(minutes);
        newTime.second(0);
        newTime.millisecond(0);
        $.ajax({
          type: "GET",
          url: "/heating/gone_out_until/" + newTime.format()
        })
        .done( function () { getNewValues( true, false ) } )
    })
  });
      if ( initial )
        setTimeout( function() { getNewValues( false, initial )}, 55000 );
    });
}

function processNewValues( data ) {
  for ( var key in data.temps ) {
    $("#"+key+"temp").html( data.temps[key].toFixed(1) + "&deg;" );
  }

  if ( data.heating ) {
    $( "#heatingstatus" ).removeClass( "bgwhite" ).addClass( "bgred" ).text( "Heating: On" );
  } else {
    $( "#heatingstatus" ).removeClass( "bgred" ).addClass( "bgwhite" ).text( "Heating: Off" );
  }

  if ( data.preheat ) {
    $( "#preheatstatus" ).removeClass( "bgwhite" ).addClass( "bgorange" ).text( "Preheat: On" );
  } else {
    $( "#preheatstatus" ).removeClass( "bgorange" ).addClass( "bgwhite" ).text( "Preheat: Off" );
  }

  if (!isNaN(parseFloat(data.currentsetpoint)) && !isNaN(parseFloat(data.override))) {
    var currentsetpoint = Number(data.currentsetpoint.toFixed(1));
    overrideTemp = Number(data.override.toFixed(1));

    if ( overrideTemp > 0 ) {
      currentsetpoint = currentsetpoint + "+" + overrideTemp + "&deg;";
    } else if ( overrideTemp < 0 ) {
      currentsetpoint = currentsetpoint + overrideTemp + "&deg;";
    } else {
      currentsetpoint = currentsetpoint + "&deg;";
    }
    $( "#currentsetpoint" ).html( currentsetpoint );

    $( "#override" ).html( "Override by " + overrideTemp + "&deg;" );
  }

  if ( data.nextsetpoint ) {
    $( "#nextsetpoint" ).html( data.nextsetpoint.toFixed(1) + "&deg;" );
  } else {
    $( "#nextsetpoint" ).text( "Unknown" ) ;
  }

  if ( data.nexteventstart ) {
    var nexteventstart = new moment( data.nexteventstart );
    $( "#nexteventstart" ).text( nexteventstart.format( "DD/MM/YYYY HH:mm" ));
  } else {
    $( "#nexteventstart" ).text( "Unknown" );
  }

  var goneOutUntil = "";
  if ( data.goneoutuntil ) {
    goneOutUntil = moment( data.goneoutuntil );
    $( "#goneoutuntil" ).val( goneOutUntil.format( "HH:mm" ) );
  } else {
    $( "#goneoutuntil" ).val( "" );
  }
}

$( document ).ready( function () {
  $( "#decrease" ).click( function() {
    overrideTemp -= 0.5;
    $.ajax({
      type: "GET",
      url: "/heating/override/" + overrideTemp
    })
      .done( function () { getNewValues( true, false ) } );
  });

  $( "#increase" ).click( function() {
    overrideTemp += 0.5;
    $.ajax({
      type: "GET",
      url: "/heating/override/" + overrideTemp
    })
      .done( function () { getNewValues( true, false ) } );
  });

  $( "#clear" ).click( function() {
    $( "#goneoutuntil" ).val( "" );
    $.ajax({
      type: "GET",
      url: "/heating/gone_out_until/null"
    })
      .done( function () { getNewValues( true, false ) } )
  });

  getNewValues( true, true );
});
