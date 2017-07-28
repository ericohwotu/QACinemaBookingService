let host = "";

window.onload = function(){
    host = window.location.hostname
    popDates();
    setInterval(refresh, 5000);
}


function selectSeat(seatId){
    //prevent submission of form

    // console.log(e.target.id.replace("seat-",""));
    // console.log(e.target.parentElement)
    //
    // let seatId = e.target.id.replace("seat-","");

    //get an ajax call to book the seat
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            changeSeatColor(document.getElementById("seat-" + seatId), JSON.parse(this.response));
        }
    };
    xhttp.open("POST", "http://" + host + ":9000/seats/json?id="+seatId , true);
    xhttp.send();
}

function changeSeatColor(elem, json){
    console.log(elem)
    elem.classList.remove("available");
    elem.classList.remove("booked");

    if(json.outcome === "failure")
        elem.classList.add("unavailable")
    else if(json.outcome === "success" && json.message==="seat booked")
        elem.classList.add("booked")
    else
        elem.classList.add("available")

}

function refresh(){
    //prevent submission of form

        //get an ajax call to book the seat
        let xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                updateButtons(JSON.parse(this.response));
            }
        };
        xhttp.open("GET", "http://" + host + ":9000/seats/json" , true);
        xhttp.send();

}

function updateButtons(arr){
    for (let i=0; i<arr.length; i++)
        updateButton(arr[i]);
}

function updateButton(json){
    let elem = document.getElementById("seat-"+json.seatid);
    elem.classList.remove("available");
    elem.classList.remove("booked");
    elem.classList.remove("unavailable");

    if(json.available === "true")
        elem.classList.add("available");
    else if(json.available === "false" && json.bookedBy === "true")
        elem.classList.add("booked");
    else {
        elem.classList.add("unavailable");
        elem.setAttribute("disabled","true");
    }
}

function enableSeats(){
    let elems = document.getElementsByClassName("fsSubmitButton");

    for(let i =0;i<elems.length;i++){
        elems[i].removeAttribute("disabled");
        elems[i].classList.remove("unavailable");
        elems[i].classList.add("available");
    }

    refresh();
}

function disableSeats(){
    let elems = document.getElementsByClassName("fsSubmitButton");

    for(let i =0;i<elems.length;i++){
        elems[i].setAttribute("disabled","true");
        elems[i].classList.remove("available");
        elems[i].classList.add("unavailable");
    }

    refresh();
}

