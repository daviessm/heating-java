var overrideTemp = Number(0.0);

function getNewValues( immediate, initial ) {
  var url = "/heating/get/all_details";
  if( immediate ) {
    url = url + "/no_wait";
  }
  var qry = $.ajax({
    method: "GET",
    url: url,
    dataType: "json",
    cache: false
  })
  .done( function( data ) {
    processNewValues( data );
  })
  .always( function( data, textStatus, jqXHR ) {
    if ( initial )
      setTimeout( function() { getNewValues( false, initial )}, 55000 );
  });
}

function processNewValues( data ) {
  if (data.allowed_update) {
    enableChanges();
  } else {
    disableChanges();
  }

  for ( var key in data.temps ) {
    let sensor = key;
    let timeout = $( "#"+sensor+"temp" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#"+sensor+"temp" ).html( data.temps[sensor].toFixed(1) + "&deg;" );
    $( "#"+sensor+"temp" ).data( "timeout", setTimeout(function() { $( "#"+sensor+"temp" ).text( "Unknown" ); }, 121000) );
  }

  if ( data.hasOwnProperty( "heating" ) ) {
    let timeout = $( "#heatingstatus" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#heatingstatus" ).data( "timeout", setTimeout(function() { $( "#heatingstatus" ).text( "Unknown" ); }, 121000 ) );

    if ( data.heating ) {
      $( "#heatingstatus" ).removeClass( "bgwhite" ).addClass( "bgred" ).text( "Heating: On" );
    } else {
      $( "#heatingstatus" ).removeClass( "bgred" ).addClass( "bgwhite" ).text( "Heating: Off" );
    }
  }

  if ( data.hasOwnProperty( "heating" ) ) {
    let timeout = $( "#preheatstatus" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#preheatstatus" ).data( "timeout", setTimeout(function() { $( "#preheatstatus" ).text( "Unknown" ); }, 121000 ) );
    if ( data.preheat ) {
      $( "#preheatstatus" ).removeClass( "bgwhite" ).addClass( "bgorange" ).text( "Preheat: On" );
    } else {
      $( "#preheatstatus" ).removeClass( "bgorange" ).addClass( "bgwhite" ).text( "Preheat: Off" );
    }
  }

  if (!isNaN(parseFloat(data.currentsetpoint)) && !isNaN(parseFloat(data.override))) {
    let timeout = $( "#currentsetpoint" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#currentsetpoint" ).data( "timeout", setTimeout(function() { $( "#currentsetpoint" ).text( "Unknown" ); }, 121000 ) );
    let currentsetpoint = Number(data.currentsetpoint.toFixed(1));
    overrideTemp = Number(data.override.toFixed(1));

    if ( overrideTemp > 0 ) {
      currentsetpoint = "" + currentsetpoint + "+" + overrideTemp + "&deg;";
    } else if ( overrideTemp < 0 ) {
      currentsetpoint = "" + currentsetpoint + overrideTemp + "&deg;";
    } else {
      currentsetpoint = "" + currentsetpoint + "&deg;";
    }
    $( "#currentsetpoint" ).html( currentsetpoint );

    $( "#override" ).html( "Override by " + overrideTemp + "&deg;" );
  }

  if ( data.nextsetpoint ) {
    let timeout = $( "#nextsetpoint" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#nextsetpoint" ).data( "timeout", setTimeout(function() { $( "#nextsetpoint" ).text( "Unknown" ); }, 121000 ) );
    $( "#nextsetpoint" ).html( data.nextsetpoint.toFixed(1) + "&deg;" );
  } else {
    $( "#nextsetpoint" ).text( "Unknown" ) ;
  }

  if ( data.nexteventstart ) {
    let timeout = $( "#nexteventstart" ).data( "timeout" );
    if ( timeout != 0 )
      clearTimeout( timeout );
    $( "#nexteventstart" ).data( "timeout", setTimeout(function() { $( "#nexteventstart" ).text( "Unknown" ); }, 121000 ) );
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

function enableChanges() {
  $( "#decrease" ).show()
    .off( "click" )
    .click( function() {
      overrideTemp -= 0.5;
      $.ajax({
        method: "POST",
        url: "/heating/set/override/" + overrideTemp
      })
        .done( function () { getNewValues( true, false ) } );
    }
  );

  $( "#increase" ).show()
    .off( "click" )
    .click( function() {
      overrideTemp += 0.5;
      $.ajax({
        method: "POST",
        url: "/heating/set/override/" + overrideTemp
      })
        .done( function () { getNewValues( true, false ) } );
    }
  );

  $( "#clear" ).show()
    .off( "click" )
    .click( function() {
      $( "#goneoutuntil" ).val( "" );
      $.ajax({
        method: "POST",
        url: "/heating/set/gone_out_until/null"
      })
        .done( function () { getNewValues( true, false ) } )
    }
  );

  if ($( "#goneoutuntil" ).hasOwnProperty( "clockTimePicker" ) ) {
    $( "#goneoutuntil" ).clockTimePicker( 'dispose' );
  }
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
        method: "POST",
        url: "/heating/set/gone_out_until/" + newTime.format()
      })
      .done( function () { getNewValues( true, false ) } )
    })
  });
}

function disableChanges() {
  $( "#decrease" ).hide()
                  .off( "click" );

  $( "#increase" ).hide()
                  .off( "click" );

  $( "#clear" ).hide()
               .off( "click" );

  if ($( "#goneoutuntil" ).hasOwnProperty( "clockTimePicker" ) ) {
    $( "#goneoutuntil" ).clockTimePicker( 'dispose' );
  }
}

$( document ).ready( function () {
  getNewValues( true, true );
});
