let host = "";
let callback;

window.onload = function () {
    host = window.location.hostname
    popDates();
}


function selectSeat(seatId) {
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            changeSeatColor(document.getElementById("seat-" + seatId), JSON.parse(this.response));
        }
    };
    xhttp.open("POST", "http://" + host + ":9000/seats/json?id=" + seatId
        + "&date=" + getSelectedText("days") + "&time=" + getSelectedText("times"), true);
    xhttp.send();
}

function isSeatLimitReached(){
    let bookedCount = document.getElementsByClassName("booked").length
    let submitBooking = document.getElementById("submit-booking")

    if(bookedCount != getTicketCount() || getTicketCount() == 0)
        submitBooking.setAttribute("disabled", "true");
    else
        submitBooking.removeAttribute("disabled");

    return bookedCount >= getTicketCount()
}

function getTicketCount(){
    let vAdult = +document.getElementById("vip-adult").value;
    let vStudent = +document.getElementById("vip-student").value;
    let vChild = +document.getElementById("vip-child").value;

    let sAdult = +document.getElementById("standard-adult").value;
    let sStudent = +document.getElementById("standard-student").value;
    let sChild = +document.getElementById("standard-child").value;

    return vAdult + vStudent + vChild + sAdult + sStudent + sChild
}

function changeSeatColor(elem, json) {
    elem.classList.remove("available");
    elem.classList.remove("booked");

    if (json.outcome === "failure")
        elem.classList.add("unavailable")
    else if (json.outcome === "success" && json.message === "seat booked")
        elem.classList.add("booked")
    else
        elem.classList.add("available")

    if (isSeatLimitReached()) disableSeats()
    else enableSeats()
}

function refresh() {
    //prevent submission of form

    //get an ajax call to book the seat
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            updateButtons(JSON.parse(this.response));
        }
    };
    xhttp.open("GET", "http://" + host + ":9000/seats/json?date=" +
        getSelectedText("days") + "&time=" + getSelectedText("times"), true);
    xhttp.send();
}

function getSelectedText(elementId) {
    let elem = document.getElementById(elementId);

    if (elem.selectedIndex == -1)
        return null;

    return elem.options[elem.selectedIndex].text;
}

function updateButtons(arr) {
    for (let i = 0; i < arr.length; i++)
        updateButton(arr[i]);
}

function updateButton(json) {
    let elem = document.getElementById("seat-" + json.seatid);
    elem.classList.remove("available");
    elem.classList.remove("booked");
    elem.classList.remove("unavailable");

    if (json.available === "true")
        elem.classList.add("available");
    else if (json.available === "false" && json.bookedBy === "true")
        elem.classList.add("booked");
    else {
        elem.classList.add("unavailable");
        elem.setAttribute("disabled", "true");
    }

    if (json.type == "EMPTY")elem.classList.add("empty");
}

function enableSeats() {
    clearInterval(callback);
    callback = setInterval(refresh, 5000);
    let elems = document.getElementsByClassName("fsSubmitButton");

    for (let i = 0; i < elems.length; i++) {
        elems[i].removeAttribute("disabled");
        // elems[i].classList.remove("unavailable");
        // elems[i].classList.add("available");
    }

    refresh();
}

function disableSeats() {
    let elems = document.getElementsByClassName("fsSubmitButton");

    for (let i = 0; i < elems.length; i++) {
        if(!elems[i].classList.contains("booked")) {
            elems[i].setAttribute("disabled", "true");
            elems[i].classList.remove("available");
            elems[i].classList.add("unavailable");
        }
    }

    clearInterval(callback);
    if(!isSeatLimitReached())refresh();
}

