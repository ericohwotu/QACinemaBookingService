//javascript function lets do this boyyyy
let days = getDays();
let hours = getTimes();

function getDays(){

    let dates = [];
    let months = ["jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"];

    for (let i=0; i<7; i++) {
        let date = new Date();
        date.setDate(date.getDate() + i)
        let today = date.getDate();
        let month = date.getMonth();
        let year = date.getYear();
        if(date.getDate()=== new Date().getDate() && date.getHours() > 21)
            dates[i];
        else
            dates[i] = today + " " + months[month].toUpperCase() + " " + (year + 1900);
    }

    if(!dates[0])
        dates.shift();

    console.log(dates);

    return dates
}

function getTimes(){
    let date = new Date();
    let timeNow = date.getHours();
    let times = new Array(7);
    let start = 0;

    if (days.length==6)
        start = 1;

    for (let i = start; i<7; i++){
        times[i] = new Array(8);
        for (let j=0; j<24; j += 3){
            if(i == 0 && j < timeNow)
                times[i][j] = 0;
            else
                times[i][j] = j + ":00";
        }
    }

    if(!times[0])
        times.shift();

    console.log(times);


    return times;
}

window.onload = function(){
    popDates();
}

function popDates(){
    let daysOptions = document.getElementById("days")
    for(let i = 0; i< days.length; i++){
        let opt = document.createElement("option");
        opt.value = i;
        opt.innerHTML = days[i];
        daysOptions.appendChild(opt);
    }
    popTimes(0)
}

function popTimes(day){
    let timesOptions = document.getElementById("times");
    timesOptions.innerHTML = "";
    timesOptions.removeAttribute("disabled");
    timesOptions.classList.remove("disabled");

    for(let i = 0; i< hours[day].length; i+=3){
        if(hours[day][i] != 0 || hours[day][i]){
            let opt = document.createElement("option");
            opt.value = i;
            opt.innerHTML = hours[day][i];
            timesOptions.appendChild(opt);
        }
    }
    if(getTicketCount()>0)refresh()
    enableScreens();
}

function enableScreens(){
    let screens = document.getElementById("screens");
    screens.removeAttribute("disabled");
    screens.classList.remove("disabled");
    if(getTicketCount()>0)refresh()
    enableTable()
}

function enableTable(){
    let tallies = document.getElementsByClassName("tally");

    for(let i=0; i<tallies.length; i++){
        tallies[i].removeAttribute("disabled");
        tallies[i].classList.remove("disabled");
    }
}

function getTotal(){
    let vAdult = document.getElementById("vip-adult").value * 18;
    let vStudent = document.getElementById("vip-student").value * 12;
    let vChild = document.getElementById("vip-child").value * 7;

    let sAdult = document.getElementById("standard-adult").value * 8;
    let sStudent = document.getElementById("standard-student").value * 6;
    let sChild = document.getElementById("standard-child").value * 3;

    let total = vAdult + vStudent + vChild + sAdult + sChild + sStudent;

    document.getElementById("total-field").value = vAdult + vStudent + vChild + sAdult + sChild + sStudent;

    if(isSeatLimitReached())disableSeats()
    else enableSeats()
}

function submitBookings(){
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            console.log(this.response);
        }
    };
    xhttp.open("POST", "http://" + host + ":9000/submit?date=" +
        getSelectedText("days") + "&time=" + getSelectedText("times"), true);
    xhttp.send();

    alert("Booking has been made, you will now be redirected to the home page")
    window.location.href = "http://www.facebook.com"
}