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

    let total = getStandardTicketCount() + getVipTicketCount();

    console.log("===============================================================")
    console.log(total)
    console.log("===============================================================")

    if(bookedCount != total || total == 0)
        submitBooking.setAttribute("disabled", "true");
    else
        submitBooking.removeAttribute("disabled");

    return bookedCount >= total
}

function isStandardLimitReached(){
    let bookedCount = document.getElementsByClassName("standard booked").length;
    console.log("isstandLi")
    console.log(bookedCount)

    isSeatLimitReached()
    return bookedCount >= getStandardTicketCount()

}

function isVipLimitReached(){
    let bookedCount = document.getElementsByClassName("vip booked").length;
    console.log("isVipLi")
    console.log(bookedCount)

    isSeatLimitReached()
    return bookedCount >= getVipTicketCount()
}

function getStandardTicketCount(){

    let sAdult = +document.getElementById("standard-adult").value;
    let sStudent = +document.getElementById("standard-student").value;
    let sChild = +document.getElementById("standard-child").value;

    return sAdult + sStudent + sChild
}

function getVipTicketCount(){
    let vAdult = +document.getElementById("vip-adult").value;
    let vStudent = +document.getElementById("vip-student").value;
    let vChild = +document.getElementById("vip-child").value;

    return vAdult + vStudent + vChild
}

function changeSeatColor(elem, json) {
    elem.classList.remove("available");
    elem.classList.remove("booked");

    console.log(json)
    if (json.outcome === "failure")
        elem.classList.add("unavailable")
    else if (json.outcome === "success" && json.message === "seat booked")
        elem.classList.add("booked")
    else
        elem.classList.add("available")

    if(isStandardLimitReached())disableStandard()
    else enableStandard();
    if(isVipLimitReached())disableVip()
    else enableVip();
}

function refresh() {
    //prevent submission of form
        console.log("refressh called")
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
    console.log("refressh called")
    for (let i = 0; i < arr.length; i++)
        updateButton(arr[i]);
}

function updateButton(json) {

    let elem = document.getElementById("seat-" + json.seatid);
    elem.classList.remove("standard");
    elem.classList.add("standard");
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

    if (json.type == "VIP")setVipButton(elem)

    if (json.type == "EMPTY")elem.classList.add("empty");

    if(isStandardLimitReached())disableStandard()
    if(isVipLimitReached())disableVip()

}

function setVipButton(seat){
    seat.classList.remove("standard")
    seat.classList.remove("vip")
    seat.classList.add("vip")
}

function disableSeats(){

}

function enableStandard() {

    clearInterval(callback);
    callback = setInterval(refresh, 5000);
    let elems = document.getElementsByClassName("standard");

    for (let i = 0; i < elems.length; i++) {
        elems[i].removeAttribute("disabled");
    }

    refresh();
}

function enableVip() {
    console.log("eableVip called")
    clearInterval(callback);
    callback = setInterval(refresh, 5000);
    let elems = document.getElementsByClassName("vip");

    console.log("enable vip calles")
    console.log(elems.length)

    for (let i = 0; i < elems.length; i++) {
        elems[i].removeAttribute("disabled");
    }

    refresh();
}

function disableStandard() {
    console.log("disableStandard called")
    let elems = document.getElementsByClassName("standard");
    console.log(elems)

    for (let i = 0; i < elems.length; i++) {
        if(!elems[i].classList.contains("booked")) {
            elems[i].setAttribute("disabled", "true");
            elems[i].classList.remove("available");
            elems[i].classList.add("unavailable");
        }

    }
    clearInterval(callback);
    //if(!isVipLimitReached()&&!isStandardLimitReached())refresh();
}

function disableVip(){
    console.log("disableVip called")
    let elems = document.getElementsByClassName("vip");

    for (let i = 0; i < elems.length; i++) {
        if(!elems[i].classList.contains("booked")) {
            elems[i].setAttribute("disabled", "true");
            elems[i].classList.remove("available");
            elems[i].classList.add("unavailable");
        }
    }

    clearInterval(callback);
    //if(!isVipLimitReached()&&!isSeatLimitReached())refresh();
}

